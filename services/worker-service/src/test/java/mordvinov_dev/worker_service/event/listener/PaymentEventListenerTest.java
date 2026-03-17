package mordvinov_dev.worker_service.event.listener;

import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.service.notification.PaymentEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "kafka.topics.payment-events=payment.events.test",
    "spring.kafka.consumer.group-id=worker-service-test"
})
class PaymentEventListenerTest {

    @Mock
    private PaymentEventProcessor eventProcessor;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

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
    void onPaymentEvent_shouldProcessEventSuccessfully() {
        paymentEventListener.onPaymentEvent(testEvent);

        verify(eventProcessor, times(1)).process(testEvent);
    }

    @Test
    void onPaymentEvent_shouldHandleFailedPaymentEvent() {
        PaymentEvent failedEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_failed_123")
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status("failed")
                .description("Payment processing failed")
                .userEmail("test@example.com")
                .build();

        paymentEventListener.onPaymentEvent(failedEvent);

        verify(eventProcessor, times(1)).process(failedEvent);
    }

    @Test
    void onPaymentEvent_shouldHandleNullSubscriptionId() {
        PaymentEvent eventWithoutSubscription = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_COMPLETED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_no_sub_123")
                .userId(UUID.randomUUID())
                .subscriptionId(null)
                .amount(new BigDecimal("49.99"))
                .currency("USD")
                .status("succeeded")
                .description("One-time payment")
                .userEmail("test@example.com")
                .build();

        paymentEventListener.onPaymentEvent(eventWithoutSubscription);

        verify(eventProcessor, times(1)).process(eventWithoutSubscription);
    }

    @Test
    void onPaymentEvent_shouldHandleEventProcessorException() {
        doThrow(new RuntimeException("Processing failed"))
                .when(eventProcessor).process(any(PaymentEvent.class));

        assertThrows(RuntimeException.class, () -> {
            paymentEventListener.onPaymentEvent(testEvent);
        });

        verify(eventProcessor, times(1)).process(testEvent);
    }

    @Test
    void onPaymentEvent_shouldHandleMultipleEvents() {
        PaymentEvent secondEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_CANCELED")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_canceled_456")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("149.99"))
                .currency("EUR")
                .status("canceled")
                .description("Payment was canceled")
                .userEmail("user@example.com")
                .build();

        paymentEventListener.onPaymentEvent(testEvent);
        paymentEventListener.onPaymentEvent(secondEvent);

        verify(eventProcessor, times(1)).process(testEvent);
        verify(eventProcessor, times(1)).process(secondEvent);
    }

    @Test
    void onPaymentEvent_shouldHandleEventWithNullUserEmail() {
        PaymentEvent eventWithoutEmail = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_PENDING")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_pending_789")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("199.99"))
                .currency("GBP")
                .status("waiting_for_capture")
                .description("Payment waiting for capture")
                .userEmail(null)
                .build();

        paymentEventListener.onPaymentEvent(eventWithoutEmail);

        verify(eventProcessor, times(1)).process(eventWithoutEmail);
    }

    @Test
    void onPaymentEvent_shouldHandleZeroAmountEvent() {
        PaymentEvent zeroAmountEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_ZERO")
                .timestamp(LocalDateTime.now())
                .paymentId("pay_zero_000")
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .status("succeeded")
                .description("Zero amount payment")
                .userEmail("zero@example.com")
                .build();

        paymentEventListener.onPaymentEvent(zeroAmountEvent);

        verify(eventProcessor, times(1)).process(zeroAmountEvent);
    }
}
