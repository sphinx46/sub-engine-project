package mordvinov_dev.worker_service.service.impl;

import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.exception.NotificationException;
import mordvinov_dev.worker_service.service.audit.AuditLogService;
import mordvinov_dev.worker_service.service.notification.NotificationService;
import mordvinov_dev.worker_service.service.notification.sender.NotificationSender;
import mordvinov_dev.worker_service.service.notification.sender.SenderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentEventProcessorImplTest {

    @Mock
    private AuditLogService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SenderRegistry senderRegistry;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private PaymentEventProcessorImpl paymentEventProcessor;

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

        when(senderRegistry.getSender(NotificationChannel.EMAIL)).thenReturn(notificationSender);
    }

    @Test
    void process_processesEventSuccessfully() {
        when(auditService.existsByEventId(testEvent.getEventId().toString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification123")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse successResponse = NotificationResponse.builder()
                .notificationId("notification123")
                .success(true)
                .message("Email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(testEvent)).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(successResponse);
        when(notificationService.markAsSent(notification)).thenReturn(successResponse);

        paymentEventProcessor.process(testEvent);

        verify(auditService, times(1)).existsByEventId(testEvent.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(testEvent);
        verify(notificationService, times(1)).createNotification(testEvent);
        verify(senderRegistry, times(1)).getSender(NotificationChannel.EMAIL);
        verify(notificationSender, times(1)).send(any(NotificationRequest.class));
        verify(notificationService, times(1)).markAsSent(notification);
    }

    @Test
    void process_skipsDuplicateEvent() {
        when(auditService.existsByEventId(testEvent.getEventId().toString())).thenReturn(true);

        paymentEventProcessor.process(testEvent);

        verify(auditService, times(1)).existsByEventId(testEvent.getEventId().toString());
        verify(auditService, never()).createAuditLog(any(PaymentEvent.class));
        verify(notificationService, never()).createNotification(any(PaymentEvent.class));
        verify(senderRegistry, never()).getSender(any(NotificationChannel.class));
        verify(notificationSender, never()).send(any(NotificationRequest.class));
    }

    @Test
    void process_handlesFailedNotification() {
        when(auditService.existsByEventId(testEvent.getEventId().toString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification456")
                .userId(testEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(testEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 99.99 USD успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse failedResponse = NotificationResponse.builder()
                .notificationId("notification456")
                .success(false)
                .message("SMTP server unavailable")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(testEvent)).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(failedResponse);
        when(notificationService.markAsFailed(notification, "SMTP server unavailable")).thenReturn(failedResponse);

        assertThrows(NotificationException.class, () -> {
            paymentEventProcessor.process(testEvent);
        });

        verify(auditService, times(1)).existsByEventId(testEvent.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(testEvent);
        verify(notificationService, times(1)).createNotification(testEvent);
        verify(senderRegistry, times(1)).getSender(NotificationChannel.EMAIL);
        verify(notificationSender, times(1)).send(any(NotificationRequest.class));
        verify(notificationService, times(1)).markAsFailed(notification, "SMTP server unavailable");
    }

    @Test
    void process_handlesNotificationException() {
        when(auditService.existsByEventId(testEvent.getEventId().toString())).thenReturn(false);
        when(notificationService.createNotification(testEvent))
                .thenThrow(new NotificationException("Failed to create notification"));

        assertThrows(NotificationException.class, () -> {
            paymentEventProcessor.process(testEvent);
        });

        verify(auditService, times(1)).existsByEventId(testEvent.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(testEvent);
        verify(notificationService, times(1)).createNotification(testEvent);
        verify(senderRegistry, never()).getSender(any(NotificationChannel.class));
    }

    @Test
    void process_handlesUnexpectedException() {
        when(auditService.existsByEventId(testEvent.getEventId().toString())).thenReturn(false);
        when(auditService.createAuditLog(testEvent))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(NotificationException.class, () -> {
            paymentEventProcessor.process(testEvent);
        });

        verify(auditService, times(1)).existsByEventId(testEvent.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(testEvent);
        verify(notificationService, never()).createNotification(any(PaymentEvent.class));
    }

    @Test
    void process_handlesEventWithNullSubscriptionId() {
        PaymentEvent eventWithoutSubscription = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_COMPLETED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_no_sub_123")
                .userId(UUID.randomUUID())
                .subscriptionId(null)
                .amount(new BigDecimal("49.99"))
                .currency("EUR")
                .status("succeeded")
                .description("One-time payment")
                .userEmail("test@example.com")
                .build();

        when(auditService.existsByEventId(eventWithoutSubscription.getEventId().toString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification789")
                .userId(eventWithoutSubscription.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(eventWithoutSubscription.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Платеж на сумму 49.99 EUR успешно выполнен")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse successResponse = NotificationResponse.builder()
                .notificationId("notification789")
                .success(true)
                .message("Email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(eventWithoutSubscription)).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(successResponse);
        when(notificationService.markAsSent(notification)).thenReturn(successResponse);

        paymentEventProcessor.process(eventWithoutSubscription);

        verify(auditService, times(1)).existsByEventId(eventWithoutSubscription.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(eventWithoutSubscription);
        verify(notificationService, times(1)).createNotification(eventWithoutSubscription);
        verify(senderRegistry, times(1)).getSender(NotificationChannel.EMAIL);
        verify(notificationSender, times(1)).send(any(NotificationRequest.class));
        verify(notificationService, times(1)).markAsSent(notification);

        verify(notificationSender, times(1)).send(argThat(request -> {
            return !request.getTemplateData().containsKey("subscriptionId");
        }));
    }

    @Test
    void process_handlesEventWithNullUserEmail() {
        PaymentEvent eventWithoutEmail = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_no_email_456")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("29.99"))
                .currency("GBP")
                .status("failed")
                .description("Payment failed")
                .userEmail(null)
                .build();

        when(auditService.existsByEventId(eventWithoutEmail.getEventId().toString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification101")
                .userId(eventWithoutEmail.getUserId())
                .type("PAYMENT_FAILED")
                .channel("EMAIL")
                .recipient("default@example.com")
                .subject("Платеж не удался")
                .content("Платеж на сумму 29.99 GBP не удался")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse successResponse = NotificationResponse.builder()
                .notificationId("notification101")
                .success(true)
                .message("Email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(eventWithoutEmail)).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(successResponse);
        when(notificationService.markAsSent(notification)).thenReturn(successResponse);

        paymentEventProcessor.process(eventWithoutEmail);

        verify(auditService, times(1)).existsByEventId(eventWithoutEmail.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(eventWithoutEmail);
        verify(notificationService, times(1)).createNotification(eventWithoutEmail);
        verify(senderRegistry, times(1)).getSender(NotificationChannel.EMAIL);
        verify(notificationSender, times(1)).send(any(NotificationRequest.class));
        verify(notificationService, times(1)).markAsSent(notification);

        verify(notificationSender, times(1)).send(argThat(request -> {
            return !request.getTemplateData().containsKey("userEmail");
        }));
    }

    @Test
    void process_handlesFailedStatusEvent() {
        PaymentEvent failedEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_failed_789")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("199.99"))
                .currency("JPY")
                .status("failed")
                .description("Payment processing failed")
                .userEmail("failed@example.com")
                .build();

        when(auditService.existsByEventId(failedEvent.getEventId().toString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification202")
                .userId(failedEvent.getUserId())
                .type("PAYMENT_FAILED")
                .channel("EMAIL")
                .recipient(failedEvent.getUserEmail())
                .subject("Платеж не удался")
                .content("Платеж на сумму 199.99 JPY не удался")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse successResponse = NotificationResponse.builder()
                .notificationId("notification202")
                .success(true)
                .message("Email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(failedEvent)).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(successResponse);
        when(notificationService.markAsSent(notification)).thenReturn(successResponse);

        paymentEventProcessor.process(failedEvent);

        verify(auditService, times(1)).existsByEventId(failedEvent.getEventId().toString());
        verify(auditService, times(1)).createAuditLog(failedEvent);
        verify(notificationService, times(1)).createNotification(failedEvent);
        verify(senderRegistry, times(1)).getSender(NotificationChannel.EMAIL);
        verify(notificationSender, times(1)).send(any(NotificationRequest.class));
        verify(notificationService, times(1)).markAsSent(notification);

        verify(notificationSender, times(1)).send(argThat(request -> {
            return "payment-failed".equals(request.getTemplateName()) &&
                   request.getTemplateData().containsKey("reason") &&
                   "Payment processing failed".equals(request.getTemplateData().get("reason"));
        }));
    }

    @Test
    void process_verifiesTemplateNamesForDifferentStatuses() {
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

        PaymentEvent canceledEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_CANCELED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_cancel_222")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .status("canceled")
                .description("Canceled payment")
                .userEmail("cancel@example.com")
                .build();

        PaymentEvent pendingEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_PENDING")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_pending_333")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("75.00"))
                .currency("GBP")
                .status("waiting_for_capture")
                .description("Pending payment")
                .userEmail("pending@example.com")
                .build();

        when(auditService.existsByEventId(anyString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification_template")
                .userId(succeededEvent.getUserId())
                .type("PAYMENT_SUCCEEDED")
                .channel("EMAIL")
                .recipient(succeededEvent.getUserEmail())
                .subject("Платеж успешно выполнен")
                .content("Payment content")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse successResponse = NotificationResponse.builder()
                .notificationId("notification_template")
                .success(true)
                .message("Email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(any(PaymentEvent.class))).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(successResponse);
        when(notificationService.markAsSent(notification)).thenReturn(successResponse);

        paymentEventProcessor.process(succeededEvent);
        paymentEventProcessor.process(canceledEvent);
        paymentEventProcessor.process(pendingEvent);

        verify(notificationSender, times(1)).send(argThat(request -> 
            "payment-success".equals(request.getTemplateName())));
        verify(notificationSender, times(1)).send(argThat(request -> 
            "payment-canceled".equals(request.getTemplateName())));
        verify(notificationSender, times(1)).send(argThat(request -> 
            "payment-pending".equals(request.getTemplateName())));
    }

    @Test
    void process_handlesUnknownStatus() {
        PaymentEvent unknownEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_UNKNOWN")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_unknown_444")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .status("unknown_status")
                .description("Unknown status")
                .userEmail("unknown@example.com")
                .build();

        when(auditService.existsByEventId(unknownEvent.getEventId().toString())).thenReturn(false);
        
        Notification notification = Notification.builder()
                .id("notification_unknown")
                .userId(unknownEvent.getUserId())
                .type("PAYMENT_UNKNOWN_STATUS")
                .channel("EMAIL")
                .recipient(unknownEvent.getUserEmail())
                .subject("Обновление статуса платежа")
                .content("Payment content")
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse successResponse = NotificationResponse.builder()
                .notificationId("notification_unknown")
                .success(true)
                .message("Email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(unknownEvent)).thenReturn(notification);
        when(notificationSender.send(any(NotificationRequest.class))).thenReturn(successResponse);
        when(notificationService.markAsSent(notification)).thenReturn(successResponse);

        paymentEventProcessor.process(unknownEvent);

        verify(notificationSender, times(1)).send(argThat(request -> 
            "default".equals(request.getTemplateName())));
    }
}
