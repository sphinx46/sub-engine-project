package mordvinov_dev.worker_service.service.impl;

import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.exception.NotificationException;
import mordvinov_dev.worker_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private PaymentEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_COMPLETED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_123456789")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status("succeeded")
                .description("Monthly subscription payment")
                .userEmail("test@example.com")
                .build();

        ReflectionTestUtils.setField(notificationService, "defaultEmailRecipient", "default@example.com");
    }

    @Test
    void createNotification_createsNotificationSuccessfully() {
        Notification expectedNotification = Notification.builder()
                .id("notification123")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен. Описание: Monthly subscription payment")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(expectedNotification);

        Notification result = notificationService.createNotification(testEvent);

        assertNotNull(result);
        assertEquals(expectedNotification.getId(), result.getId());
        assertEquals(expectedNotification.getUserId(), result.getUserId());
        assertEquals(expectedNotification.getType(), result.getType());
        assertEquals(expectedNotification.getChannel(), result.getChannel());
        assertEquals(expectedNotification.getRecipient(), result.getRecipient());
        assertEquals(expectedNotification.getSubject(), result.getSubject());
        assertEquals(expectedNotification.getContent(), result.getContent());
        assertEquals(expectedNotification.getSent(), result.getSent());
        assertNotNull(result.getCreatedAt());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_usesDefaultEmailWhenUserEmailIsNull() {
        PaymentEvent eventWithNullEmail = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_failed_123")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("49.99"))
                .currency("EUR")
                .status("failed")
                .description("Payment failed")
                .userEmail(null)
                .build();

        Notification expectedNotification = Notification.builder()
                .id("notification456")
                .userId(eventWithNullEmail.getUserId())
                .type("PAYMENT_FAILED")
                .channel("EMAIL")
                .recipient("default@example.com")
                .subject("Платеж не удался")
                .content("Платеж на сумму 49.99 EUR не удался. Описание: Payment failed")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(expectedNotification);

        Notification result = notificationService.createNotification(eventWithNullEmail);

        assertNotNull(result);
        assertEquals("default@example.com", result.getRecipient());
        assertEquals("PAYMENT_FAILED", result.getType());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_usesDefaultEmailWhenUserEmailIsEmpty() {
        PaymentEvent eventWithEmptyEmail = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_CANCELED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_canceled_456")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("29.99"))
                .currency("GBP")
                .status("canceled")
                .description("Payment canceled")
                .userEmail("")
                .build();

        Notification expectedNotification = Notification.builder()
                .id("notification789")
                .userId(eventWithEmptyEmail.getUserId())
                .type("PAYMENT_CANCELED")
                .channel("EMAIL")
                .recipient("default@example.com")
                .subject("Платеж отменен")
                .content("Платеж на сумму 29.99 GBP отменен. Описание: Payment canceled")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(expectedNotification);

        Notification result = notificationService.createNotification(eventWithEmptyEmail);

        assertNotNull(result);
        assertEquals("default@example.com", result.getRecipient());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_handlesNullDescription() {
        PaymentEvent eventWithNullDescription = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_PENDING")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_pending_789")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("199.99"))
                .currency("JPY")
                .status("waiting_for_capture")
                .description(null)
                .userEmail("pending@example.com")
                .build();

        Notification expectedNotification = Notification.builder()
                .id("notification101")
                .userId(eventWithNullDescription.getUserId())
                .type("PAYMENT_WAITING_FOR_CAPTURE")
                .channel("EMAIL")
                .recipient(eventWithNullDescription.getUserEmail())
                .subject("Платеж ожидает подтверждения")
                .content("Платеж на сумму 199.99 JPY ожидает подтверждения. ")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(expectedNotification);

        Notification result = notificationService.createNotification(eventWithNullDescription);

        assertNotNull(result);
        assertTrue(result.getContent().endsWith(". "));

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_handlesUnknownStatus() {
        PaymentEvent eventWithUnknownStatus = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_UNKNOWN")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_unknown_111")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("79.99"))
                .currency("USD")
                .status("unknown_status")
                .description("Unknown status")
                .userEmail("unknown@example.com")
                .build();

        Notification expectedNotification = Notification.builder()
                .id("notification202")
                .userId(eventWithUnknownStatus.getUserId())
                .type("PAYMENT_UNKNOWN_STATUS")
                .channel("EMAIL")
                .recipient(eventWithUnknownStatus.getUserEmail())
                .subject("Обновление статуса платежа")
                .content("Платеж на сумму 79.99 USD unknown_status. Описание: Unknown status")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(expectedNotification);

        Notification result = notificationService.createNotification(eventWithUnknownStatus);

        assertNotNull(result);
        assertEquals("Обновление статуса платежа", result.getSubject());
        assertEquals("PAYMENT_UNKNOWN_STATUS", result.getType());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_handlesRepositoryException() {
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(NotificationException.class, () -> {
            notificationService.createNotification(testEvent);
        });

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void markAsSent_marksNotificationAsSentSuccessfully() {
        Notification notification = Notification.builder()
                .id("notification303")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification updatedNotification = Notification.builder()
                .id("notification303")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(updatedNotification);

        NotificationResponse result = notificationService.markAsSent(notification);

        assertNotNull(result);
        assertEquals("notification303", result.getNotificationId());
        assertTrue(result.isSuccess());
        assertEquals("Notification sent successfully", result.getMessage());
        assertNotNull(result.getTimestamp());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void markAsSent_handlesRepositoryException() {
        Notification notification = Notification.builder()
                .id("notification404")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(NotificationException.class, () -> {
            notificationService.markAsSent(notification);
        });

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void markAsFailed_marksNotificationAsFailedSuccessfully() {
        Notification notification = Notification.builder()
                .id("notification505")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        String errorMessage = "SMTP server unavailable";
        Notification updatedNotification = Notification.builder()
                .id("notification505")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(updatedNotification);

        NotificationResponse result = notificationService.markAsFailed(notification, errorMessage);

        assertNotNull(result);
        assertEquals("notification505", result.getNotificationId());
        assertFalse(result.isSuccess());
        assertEquals("Failed: " + errorMessage, result.getMessage());
        assertNotNull(result.getTimestamp());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void markAsFailed_handlesRepositoryException() {
        Notification notification = Notification.builder()
                .id("notification606")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(NotificationException.class, () -> {
            notificationService.markAsFailed(notification, "Error message");
        });

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_verifiesSubjectForDifferentStatuses() {
        PaymentEvent succeededEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_SUCCEEDED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_success_111")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("succeeded")
                .description("Success payment")
                .userEmail("success@example.com")
                .build();

        PaymentEvent failedEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_fail_222")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .status("failed")
                .description("Failed payment")
                .userEmail("fail@example.com")
                .build();

        PaymentEvent canceledEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_CANCELED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_cancel_333")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("25.00"))
                .currency("GBP")
                .status("canceled")
                .description("Canceled payment")
                .userEmail("cancel@example.com")
                .build();

        PaymentEvent pendingEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_PENDING")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_pending_444")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("75.00"))
                .currency("JPY")
                .status("waiting_for_capture")
                .description("Pending payment")
                .userEmail("pending@example.com")
                .build();

        Notification successNotification = Notification.builder()
                .id("notif_success")
                .subject("Платеж успешно выполнен")
                .build();

        Notification failNotification = Notification.builder()
                .id("notif_fail")
                .subject("Платеж не удался")
                .build();

        Notification cancelNotification = Notification.builder()
                .id("notif_cancel")
                .subject("Платеж отменен")
                .build();

        Notification pendingNotification = Notification.builder()
                .id("notif_pending")
                .subject("Платеж ожидает подтверждения")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(successNotification)
                .thenReturn(failNotification)
                .thenReturn(cancelNotification)
                .thenReturn(pendingNotification);

        Notification result1 = notificationService.createNotification(succeededEvent);
        Notification result2 = notificationService.createNotification(failedEvent);
        Notification result3 = notificationService.createNotification(canceledEvent);
        Notification result4 = notificationService.createNotification(pendingEvent);

        assertEquals("Платеж успешно выполнен", result1.getSubject());
        assertEquals("Платеж не удался", result2.getSubject());
        assertEquals("Платеж отменен", result3.getSubject());
        assertEquals("Платеж ожидает подтверждения", result4.getSubject());

        verify(notificationRepository, times(4)).save(any(Notification.class));
    }
}
