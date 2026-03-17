package mordvinov_dev.subscription_service.listener;

import mordvinov_dev.subscription_service.event.PremiumSubscriptionResponseEvent;
import mordvinov_dev.subscription_service.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PremiumSubscriptionResponseListener Tests")
class PremiumSubscriptionResponseListenerTest {

    @Mock
    private SubscriptionServiceImpl subscriptionService;

    @InjectMocks
    private PremiumSubscriptionResponseListener listener;

    private PremiumSubscriptionResponseEvent successEvent;
    private PremiumSubscriptionResponseEvent failedEvent;
    private PremiumSubscriptionResponseEvent pendingEvent;
    private UUID subscriptionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        userId = UUID.randomUUID();

        successEvent = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId("payment-123")
                .confirmationUrl("https://payment.example.com/confirm/123")
                .status("SUCCESS")
                .message("Payment successful")
                .build();

        failedEvent = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId("payment-456")
                .confirmationUrl(null)
                .status("FAILED")
                .message("Payment declined")
                .build();

        pendingEvent = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId("payment-789")
                .confirmationUrl("https://payment.example.com/confirm/789")
                .status("PENDING")
                .message("Payment processing")
                .build();
    }

    @Test
    @DisplayName("Should activate subscription when payment status is SUCCESS")
    void shouldActivateSubscriptionWhenPaymentStatusIsSuccess() {
        listener.onPremiumSubscriptionResponse(successEvent);

        verify(subscriptionService, times(1)).activatePremiumSubscription(subscriptionId);
        verify(subscriptionService, never()).failPremiumSubscription(any());
    }

    @Test
    @DisplayName("Should fail subscription when payment status is FAILED")
    void shouldFailSubscriptionWhenPaymentStatusIsFailed() {
        listener.onPremiumSubscriptionResponse(failedEvent);

        verify(subscriptionService, times(1)).failPremiumSubscription(subscriptionId);
        verify(subscriptionService, never()).activatePremiumSubscription(any());
    }

    @Test
    @DisplayName("Should not call service methods when payment status is PENDING")
    void shouldNotCallServiceMethodsWhenPaymentStatusIsPending() {
        listener.onPremiumSubscriptionResponse(pendingEvent);

        verify(subscriptionService, never()).activatePremiumSubscription(any());
        verify(subscriptionService, never()).failPremiumSubscription(any());
    }

    @Test
    @DisplayName("Should handle unknown status gracefully")
    void shouldHandleUnknownStatusGracefully() {
        PremiumSubscriptionResponseEvent unknownEvent = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId("payment-999")
                .status("UNKNOWN")
                .message("Unknown status")
                .build();

        listener.onPremiumSubscriptionResponse(unknownEvent);

        verify(subscriptionService, never()).activatePremiumSubscription(any());
        verify(subscriptionService, never()).failPremiumSubscription(any());
    }

    @Test
    @DisplayName("Should handle exceptions gracefully when service throws exception")
    void shouldHandleExceptionsGracefullyWhenServiceThrowsException() {
        RuntimeException exception = new RuntimeException("Database error");
        doThrow(exception).when(subscriptionService).activatePremiumSubscription(subscriptionId);

        assertDoesNotThrow(() -> listener.onPremiumSubscriptionResponse(successEvent));

        verify(subscriptionService, times(1)).activatePremiumSubscription(subscriptionId);
    }

    @Test
    @DisplayName("Should handle exceptions gracefully when fail service throws exception")
    void shouldHandleExceptionsGracefullyWhenFailServiceThrowsException() {
        RuntimeException exception = new RuntimeException("Database error");
        doThrow(exception).when(subscriptionService).failPremiumSubscription(subscriptionId);

        assertDoesNotThrow(() -> listener.onPremiumSubscriptionResponse(failedEvent));

        verify(subscriptionService, times(1)).failPremiumSubscription(subscriptionId);
    }

    @Test
    @DisplayName("Should process multiple events correctly")
    void shouldProcessMultipleEventsCorrectly() {
        listener.onPremiumSubscriptionResponse(successEvent);
        listener.onPremiumSubscriptionResponse(failedEvent);
        listener.onPremiumSubscriptionResponse(pendingEvent);

        verify(subscriptionService, times(1)).activatePremiumSubscription(successEvent.getSubscriptionId());
        verify(subscriptionService, times(1)).failPremiumSubscription(failedEvent.getSubscriptionId());

        verify(subscriptionService, times(1)).activatePremiumSubscription(any());
        verify(subscriptionService, times(1)).failPremiumSubscription(any());
    }

    @Test
    @DisplayName("Should handle events with null fields gracefully")
    void shouldHandleEventsWithNullFieldsGracefully() {
        PremiumSubscriptionResponseEvent eventWithNulls = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId(null)
                .confirmationUrl(null)
                .status("SUCCESS")
                .message(null)
                .build();

        assertDoesNotThrow(() -> listener.onPremiumSubscriptionResponse(eventWithNulls));

        verify(subscriptionService, times(1)).activatePremiumSubscription(subscriptionId);
    }

    @Test
    @DisplayName("Should handle events with null status gracefully")
    void shouldHandleEventsWithNullStatusGracefully() {
        PremiumSubscriptionResponseEvent eventWithNullStatus = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId("payment-null")
                .confirmationUrl(null)
                .status(null)
                .message("No status")
                .build();

        assertDoesNotThrow(() -> listener.onPremiumSubscriptionResponse(eventWithNullStatus));

        verify(subscriptionService, never()).activatePremiumSubscription(any());
        verify(subscriptionService, never()).failPremiumSubscription(any());
    }
}
