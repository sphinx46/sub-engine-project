package mordvinov_dev.worker_service.integration;

import mordvinov_dev.worker_service.domain.document.AuditLog;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.repository.AuditLogRepository;
import mordvinov_dev.worker_service.repository.NotificationRepository;
import mordvinov_dev.worker_service.service.notification.PaymentEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EmbeddedKafka(partitions = 1, topics = {"payment.events.test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class WorkerServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Autowired
    private PaymentEventProcessor paymentEventProcessor;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private mordvinov_dev.worker_service.service.notification.sender.NotificationSender mockEmailSender;

    private static final String TEST_TOPIC = "payment.events.test";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(AuditLog.class);
        mongoTemplate.dropCollection(Notification.class);
        reset(mockEmailSender);
        when(mockEmailSender.send(any())).thenReturn(
                mordvinov_dev.worker_service.domain.dto.response.NotificationResponse.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .success(true)
                        .message("Test success")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
        when(mockEmailSender.getChannel()).thenReturn(mordvinov_dev.worker_service.domain.document.enums.NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("Successful payment event processing - end to end")
    void successfulPaymentEvent_processingEndToEnd() throws Exception {
        PaymentEvent event = createTestPaymentEvent("succeeded");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertEquals(1, auditLogs.size());
            assertEquals(event.getEventId().toString(), auditLogs.get(0).getEventId().toString());
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertTrue(notifications.get(0).getSent());
            assertEquals("PAYMENT_SUCCEEDED", notifications.get(0).getType());
            assertEquals(event.getUserEmail(), notifications.get(0).getRecipient());
        });

        verify(mockEmailSender, times(1)).send(any());
    }

    @Test
    @DisplayName("Failed payment event processing - end to end")
    void failedPaymentEvent_processingEndToEnd() throws Exception {
        PaymentEvent event = createTestPaymentEvent("failed");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertEquals(1, auditLogs.size());
            assertEquals(event.getEventId().toString(), auditLogs.get(0).getEventId().toString());
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertTrue(notifications.get(0).getSent());
            assertEquals("PAYMENT_FAILED", notifications.get(0).getType());
        });

        verify(mockEmailSender, times(1)).send(any());
    }

    @Test
    @DisplayName("Canceled payment event processing")
    void canceledPaymentEvent_processing() throws Exception {
        PaymentEvent event = createTestPaymentEvent("canceled");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertEquals("PAYMENT_CANCELED", notifications.get(0).getType());
        });
    }

    @Test
    @DisplayName("Pending payment event processing")
    void pendingPaymentEvent_processing() throws Exception {
        PaymentEvent event = createTestPaymentEvent("waiting_for_capture");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertEquals("PAYMENT_WAITING_FOR_CAPTURE", notifications.get(0).getType());
        });
    }

    @Test
    @DisplayName("Unknown payment status - default template")
    void unknownPaymentStatus_usesDefaultTemplate() throws Exception {
        PaymentEvent event = createTestPaymentEvent("unknown_status");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertEquals("PAYMENT_UNKNOWN_STATUS", notifications.get(0).getType());
        });
    }

    @Test
    @DisplayName("Duplicate event - should not process twice")
    void duplicateEvent_shouldNotProcessTwice() throws Exception {
        PaymentEvent event = createTestPaymentEvent("succeeded");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);
        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertEquals(1, auditLogs.size());
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
        });

        verify(mockEmailSender, times(1)).send(any());
    }

    @Test
    @DisplayName("Event without user email - uses default recipient")
    void eventWithoutUserEmail_usesDefaultRecipient() throws Exception {
        PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("payment")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_123")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("succeeded")
                .description("Test payment")
                .userEmail(null)
                .build();

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertEquals("test@example.com", notifications.get(0).getRecipient());
        });
    }

    @Test
    @DisplayName("Notification sender failure - marks notification as failed")
    void notificationSenderFailure_marksNotificationAsFailed() throws Exception {
        when(mockEmailSender.send(any())).thenReturn(
                mordvinov_dev.worker_service.domain.dto.response.NotificationResponse.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .success(false)
                        .message("SMTP server error")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        PaymentEvent event = createTestPaymentEvent("succeeded");

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertFalse(notifications.get(0).getSent());
        });
    }

    @Test
    @DisplayName("Event with subscription ID - includes in template data")
    void eventWithSubscriptionId_includesInTemplateData() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("payment")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_123")
                .userId(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .status("succeeded")
                .description("Subscription payment")
                .userEmail("test@example.com")
                .build();

        kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertTrue(notifications.get(0).getSent());
        });

        verify(mockEmailSender, times(1)).send(argThat(request ->
                request.getTemplateData().containsKey("subscriptionId") &&
                        request.getTemplateData().get("subscriptionId").equals(subscriptionId.toString())
        ));
    }

    @Test
    @DisplayName("Direct processor call - bypass Kafka")
    void directProcessorCall_bypassKafka() {
        PaymentEvent event = createTestPaymentEvent("succeeded");

        paymentEventProcessor.process(event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertEquals(1, auditLogs.size());
            assertEquals(event.getEventId().toString(), auditLogs.get(0).getEventId().toString());
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(1, notifications.size());
            assertTrue(notifications.get(0).getSent());
        });

        verify(mockEmailSender, times(1)).send(any());
    }

    @Test
    @DisplayName("Multiple events processing - concurrent")
    void multipleEventsProcessing_concurrent() throws Exception {
        int eventCount = 5;
        for (int i = 0; i < eventCount; i++) {
            PaymentEvent event = createTestPaymentEvent("succeeded");
            event.setEventId(UUID.randomUUID());
            event.setPaymentId("pay_" + i);

            kafkaTemplate.send(TEST_TOPIC, event.getEventId().toString(), event);
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertEquals(eventCount, auditLogs.size());
        });

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertEquals(eventCount, notifications.size());
            assertTrue(notifications.stream().allMatch(n -> n.getSent()));
        });

        verify(mockEmailSender, times(eventCount)).send(any());
    }

    private PaymentEvent createTestPaymentEvent(String status) {
        return PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("payment")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_test_123")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(status)
                .description("Test payment")
                .userEmail("test@example.com")
                .build();
    }
}