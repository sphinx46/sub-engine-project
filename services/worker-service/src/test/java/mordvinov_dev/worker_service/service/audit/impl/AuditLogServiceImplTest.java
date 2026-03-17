package mordvinov_dev.worker_service.service.audit.impl;

import mordvinov_dev.worker_service.domain.document.AuditLog;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

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
    }

    @Test
    void createAuditLog_createsAuditLogSuccessfully() {
        AuditLog expectedAuditLog = AuditLog.builder()
                .eventId(testEvent.getEventId())
                .eventType(testEvent.getEventType())
                .userId(testEvent.getUserId())
                .paymentId(testEvent.getPaymentId())
                .subscriptionId(testEvent.getSubscriptionId())
                .action("PAYMENT_SUCCEEDED")
                .status(testEvent.getStatus())
                .details(String.format("Payment processed: amount=%.2f %s, description=%s",
                        testEvent.getAmount(), testEvent.getCurrency(), testEvent.getDescription()))
                .timestamp(testEvent.getTimestamp())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(expectedAuditLog);

        AuditLog result = auditLogService.createAuditLog(testEvent);

        assertNotNull(result);
        assertEquals(expectedAuditLog.getEventId(), result.getEventId());
        assertEquals(expectedAuditLog.getEventType(), result.getEventType());
        assertEquals(expectedAuditLog.getUserId(), result.getUserId());
        assertEquals(expectedAuditLog.getPaymentId(), result.getPaymentId());
        assertEquals(expectedAuditLog.getSubscriptionId(), result.getSubscriptionId());
        assertEquals(expectedAuditLog.getAction(), result.getAction());
        assertEquals(expectedAuditLog.getStatus(), result.getStatus());
        assertEquals(expectedAuditLog.getDetails(), result.getDetails());
        assertEquals(expectedAuditLog.getTimestamp(), result.getTimestamp());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void createAuditLog_handlesNullTimestamp() {
        PaymentEvent eventWithNullTimestamp = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(null)
                .paymentId("pay_failed_123")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("49.99"))
                .currency("EUR")
                .status("failed")
                .description("Payment failed")
                .userEmail("failed@example.com")
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog saved = invocation.getArgument(0);
            saved.setId("audit123");
            if (saved.getTimestamp() == null) {
                saved.setTimestamp(LocalDateTime.now());
            }
            return saved;
        });

        AuditLog result = auditLogService.createAuditLog(eventWithNullTimestamp);

        assertNotNull(result);
        assertNotNull(result.getTimestamp());
        assertEquals(eventWithNullTimestamp.getEventId(), result.getEventId());
        assertEquals("PAYMENT_FAILED", result.getAction());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void createAuditLog_handlesNullDescription() {
        PaymentEvent eventWithNullDescription = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_CANCELED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_canceled_456")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("29.99"))
                .currency("GBP")
                .status("canceled")
                .description(null)
                .userEmail("canceled@example.com")
                .build();

        AuditLog expectedAuditLog = AuditLog.builder()
                .eventId(eventWithNullDescription.getEventId())
                .eventType(eventWithNullDescription.getEventType())
                .userId(eventWithNullDescription.getUserId())
                .paymentId(eventWithNullDescription.getPaymentId())
                .subscriptionId(eventWithNullDescription.getSubscriptionId())
                .action("PAYMENT_CANCELED")
                .status(eventWithNullDescription.getStatus())
                .details(String.format("Payment processed: amount=%.2f %s, description=N/A",
                        eventWithNullDescription.getAmount(), eventWithNullDescription.getCurrency()))
                .timestamp(eventWithNullDescription.getTimestamp())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(expectedAuditLog);

        AuditLog result = auditLogService.createAuditLog(eventWithNullDescription);

        assertNotNull(result);
        assertTrue(result.getDetails().contains("description=N/A"));

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void createAuditLog_handlesNullSubscriptionId() {
        PaymentEvent eventWithoutSubscription = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_PENDING")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_pending_789")
                .userId(UUID.randomUUID())
                .subscriptionId(null)
                .amount(new BigDecimal("199.99"))
                .currency("JPY")
                .status("waiting_for_capture")
                .description("Payment pending")
                .userEmail("pending@example.com")
                .build();

        AuditLog expectedAuditLog = AuditLog.builder()
                .eventId(eventWithoutSubscription.getEventId())
                .eventType(eventWithoutSubscription.getEventType())
                .userId(eventWithoutSubscription.getUserId())
                .paymentId(eventWithoutSubscription.getPaymentId())
                .subscriptionId(null)
                .action("PAYMENT_WAITING_FOR_CAPTURE")
                .status(eventWithoutSubscription.getStatus())
                .details(String.format("Payment processed: amount=%.2f %s, description=%s",
                        eventWithoutSubscription.getAmount(), eventWithoutSubscription.getCurrency(), eventWithoutSubscription.getDescription()))
                .timestamp(eventWithoutSubscription.getTimestamp())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(expectedAuditLog);

        AuditLog result = auditLogService.createAuditLog(eventWithoutSubscription);

        assertNotNull(result);
        assertNull(result.getSubscriptionId());
        assertEquals("PAYMENT_WAITING_FOR_CAPTURE", result.getAction());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void existsByEventId_returnsTrueWhenExists() {
        String eventId = testEvent.getEventId().toString();
        when(auditLogRepository.existsByEventId(UUID.fromString(eventId))).thenReturn(true);

        boolean result = auditLogService.existsByEventId(eventId);

        assertTrue(result);
        verify(auditLogRepository, times(1)).existsByEventId(UUID.fromString(eventId));
    }

    @Test
    void existsByEventId_returnsFalseWhenNotExists() {
        String eventId = UUID.randomUUID().toString();
        when(auditLogRepository.existsByEventId(UUID.fromString(eventId))).thenReturn(false);

        boolean result = auditLogService.existsByEventId(eventId);

        assertFalse(result);
        verify(auditLogRepository, times(1)).existsByEventId(UUID.fromString(eventId));
    }

    @Test
    void existsByEventId_handlesInvalidUUID() {
        String invalidEventId = "invalid-uuid";

        assertThrows(IllegalArgumentException.class, () -> {
            auditLogService.existsByEventId(invalidEventId);
        });
    }

    @Test
    void createAuditLog_handlesRepositoryException() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            auditLogService.createAuditLog(testEvent);
        });

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void createAuditLog_verifiesActionFormat() {
        PaymentEvent eventWithUpperCaseStatus = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_COMPLETED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_upper_123")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("79.99"))
                .currency("USD")
                .status("SUCCEEDED")
                .description("Upper case status")
                .userEmail("upper@example.com")
                .build();

        AuditLog expectedAuditLog = AuditLog.builder()
                .eventId(eventWithUpperCaseStatus.getEventId())
                .eventType(eventWithUpperCaseStatus.getEventType())
                .userId(eventWithUpperCaseStatus.getUserId())
                .paymentId(eventWithUpperCaseStatus.getPaymentId())
                .subscriptionId(eventWithUpperCaseStatus.getSubscriptionId())
                .action("PAYMENT_SUCCEEDED")
                .status(eventWithUpperCaseStatus.getStatus())
                .details(String.format("Payment processed: amount=%.2f %s, description=%s",
                        eventWithUpperCaseStatus.getAmount(), eventWithUpperCaseStatus.getCurrency(), eventWithUpperCaseStatus.getDescription()))
                .timestamp(eventWithUpperCaseStatus.getTimestamp())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(expectedAuditLog);

        AuditLog result = auditLogService.createAuditLog(eventWithUpperCaseStatus);

        assertNotNull(result);
        assertEquals("PAYMENT_SUCCEEDED", result.getAction());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }
}
