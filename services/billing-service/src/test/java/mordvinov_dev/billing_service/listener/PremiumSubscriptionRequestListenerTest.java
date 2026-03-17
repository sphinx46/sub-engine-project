package mordvinov_dev.billing_service.listener;

import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.event.PremiumSubscriptionRequestEvent;
import mordvinov_dev.billing_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PremiumSubscriptionRequestListener Tests")
class PremiumSubscriptionRequestListenerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PremiumSubscriptionRequestListener listener;

    private PremiumSubscriptionRequestEvent testEvent;
    private PremiumSubscriptionRequestEvent eventWithEmail;
    private PremiumSubscriptionRequestEvent eventWithoutEmail;
    private UUID subscriptionId;
    private UUID userId;
    private UUID eventId;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        testEvent = PremiumSubscriptionRequestEvent.builder()
                .eventId(eventId)
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Premium subscription")
                .userEmail("test@example.com")
                .build();

        eventWithEmail = PremiumSubscriptionRequestEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .currency("RUB")
                .description("Premium subscription with email")
                .userEmail("user@example.com")
                .build();

        eventWithoutEmail = PremiumSubscriptionRequestEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("750.00"))
                .currency("RUB")
                .description("Premium subscription without email")
                .userEmail(null)
                .build();

        paymentResponse = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .paymentId("payment-123")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .status("pending")
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Premium subscription")
                .confirmationUrl("https://payment.example.com/confirm/123")
                .build();
    }

    @Test
    @DisplayName("Should process premium subscription request successfully")
    void shouldProcessPremiumSubscriptionRequestSuccessfully() {
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(userId)))
                .thenReturn(paymentResponse);

        listener.onPremiumSubscriptionRequest(testEvent);

        ArgumentCaptor<CreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService, times(1)).createPayment(requestCaptor.capture(), eq(userId));

        CreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertEquals(new BigDecimal("1000.00"), capturedRequest.getAmount());
        assertEquals("RUB", capturedRequest.getCurrency());
        assertEquals("Premium subscription", capturedRequest.getDescription());
        assertEquals(subscriptionId, capturedRequest.getSubscriptionId());
        assertTrue(capturedRequest.getCapture());
        assertFalse(capturedRequest.getSavePaymentMethod());
        assertNotNull(capturedRequest.getMetadata());
        assertEquals(userId.toString(), capturedRequest.getMetadata().get("userId"));
        assertEquals(subscriptionId.toString(), capturedRequest.getMetadata().get("subscriptionId"));
        assertEquals(eventId.toString(), capturedRequest.getMetadata().get("eventId"));
        assertEquals("test@example.com", capturedRequest.getMetadata().get("userEmail"));
    }

    @Test
    @DisplayName("Should process request without email in metadata")
    void shouldProcessRequestWithoutEmailInMetadata() {
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(eventWithoutEmail.getUserId())))
                .thenReturn(paymentResponse);

        listener.onPremiumSubscriptionRequest(eventWithoutEmail);

        ArgumentCaptor<CreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService, times(1)).createPayment(requestCaptor.capture(), eq(eventWithoutEmail.getUserId()));

        CreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getMetadata());
        assertEquals(eventWithoutEmail.getUserId().toString(), capturedRequest.getMetadata().get("userId"));
        assertEquals(eventWithoutEmail.getSubscriptionId().toString(), capturedRequest.getMetadata().get("subscriptionId"));
        assertEquals(eventWithoutEmail.getEventId().toString(), capturedRequest.getMetadata().get("eventId"));
        assertFalse(capturedRequest.getMetadata().containsKey("userEmail"));
    }

    @Test
    @DisplayName("Should process request with empty email")
    void shouldProcessRequestWithEmptyEmail() {
        PremiumSubscriptionRequestEvent eventWithEmptyEmail = PremiumSubscriptionRequestEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Premium subscription")
                .userEmail("")
                .build();

        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(eventWithEmptyEmail.getUserId())))
                .thenReturn(paymentResponse);

        listener.onPremiumSubscriptionRequest(eventWithEmptyEmail);

        ArgumentCaptor<CreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService, times(1)).createPayment(requestCaptor.capture(), eq(eventWithEmptyEmail.getUserId()));

        CreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertFalse(capturedRequest.getMetadata().containsKey("userEmail"));
    }

    @Test
    @DisplayName("Should handle payment service exception gracefully")
    void shouldHandlePaymentServiceExceptionGracefully() {
        RuntimeException exception = new RuntimeException("Payment service error");
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(userId)))
                .thenThrow(exception);

        assertDoesNotThrow(() -> listener.onPremiumSubscriptionRequest(testEvent));

        verify(paymentService, times(1)).createPayment(any(CreatePaymentRequest.class), eq(userId));
    }

    @Test
    @DisplayName("Should process multiple requests correctly")
    void shouldProcessMultipleRequestsCorrectly() {
        PaymentResponse response1 = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .paymentId("payment-1")
                .userId(eventWithEmail.getUserId())
                .subscriptionId(eventWithEmail.getSubscriptionId())
                .status("pending")
                .amount(eventWithEmail.getAmount())
                .currency(eventWithEmail.getCurrency())
                .description(eventWithEmail.getDescription())
                .confirmationUrl("https://payment.example.com/confirm/1")
                .build();

        PaymentResponse response2 = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .paymentId("payment-2")
                .userId(eventWithoutEmail.getUserId())
                .subscriptionId(eventWithoutEmail.getSubscriptionId())
                .status("pending")
                .amount(eventWithoutEmail.getAmount())
                .currency(eventWithoutEmail.getCurrency())
                .description(eventWithoutEmail.getDescription())
                .confirmationUrl("https://payment.example.com/confirm/2")
                .build();

        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(eventWithEmail.getUserId())))
                .thenReturn(response1);
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(eventWithoutEmail.getUserId())))
                .thenReturn(response2);

        listener.onPremiumSubscriptionRequest(eventWithEmail);
        listener.onPremiumSubscriptionRequest(eventWithoutEmail);

        verify(paymentService, times(2)).createPayment(any(CreatePaymentRequest.class), any());
    }

    @Test
    @DisplayName("Should handle event with null fields gracefully")
    void shouldHandleEventWithNullFieldsGracefully() {
        PremiumSubscriptionRequestEvent eventWithNulls = PremiumSubscriptionRequestEvent.builder()
                .eventId(null)
                .eventType(null)
                .timestamp(null)
                .subscriptionId(null)
                .userId(null)
                .amount(null)
                .currency(null)
                .description(null)
                .userEmail(null)
                .build();

        assertDoesNotThrow(() -> listener.onPremiumSubscriptionRequest(eventWithNulls));
    }

    @Test
    @DisplayName("Should create correct metadata structure")
    void shouldCreateCorrectMetadataStructure() {
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(userId)))
                .thenReturn(paymentResponse);

        listener.onPremiumSubscriptionRequest(testEvent);

        ArgumentCaptor<CreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService, times(1)).createPayment(requestCaptor.capture(), eq(userId));

        CreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getMetadata());
        assertEquals(4, capturedRequest.getMetadata().size());
        assertTrue(capturedRequest.getMetadata().containsKey("userId"));
        assertTrue(capturedRequest.getMetadata().containsKey("subscriptionId"));
        assertTrue(capturedRequest.getMetadata().containsKey("eventId"));
        assertTrue(capturedRequest.getMetadata().containsKey("userEmail"));
    }

    @Test
    @DisplayName("Should pass correct user ID to payment service")
    void shouldPassCorrectUserIdToPaymentService() {
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(userId)))
                .thenReturn(paymentResponse);

        listener.onPremiumSubscriptionRequest(testEvent);

        verify(paymentService, times(1)).createPayment(any(CreatePaymentRequest.class), eq(userId));
    }

    @Test
    @DisplayName("Should handle different currencies and amounts")
    void shouldHandleDifferentCurrenciesAndAmounts() {
        PremiumSubscriptionRequestEvent usdEvent = PremiumSubscriptionRequestEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("15.99"))
                .currency("USD")
                .description("Premium subscription USD")
                .userEmail("user@example.com")
                .build();

        PaymentResponse usdResponse = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .paymentId("payment-usd")
                .userId(usdEvent.getUserId())
                .subscriptionId(usdEvent.getSubscriptionId())
                .status("pending")
                .amount(usdEvent.getAmount())
                .currency(usdEvent.getCurrency())
                .description(usdEvent.getDescription())
                .confirmationUrl("https://payment.example.com/confirm/usd")
                .build();

        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(usdEvent.getUserId())))
                .thenReturn(usdResponse);

        listener.onPremiumSubscriptionRequest(usdEvent);

        ArgumentCaptor<CreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService, times(1)).createPayment(requestCaptor.capture(), eq(usdEvent.getUserId()));

        CreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertEquals(new BigDecimal("15.99"), capturedRequest.getAmount());
        assertEquals("USD", capturedRequest.getCurrency());
        assertEquals("Premium subscription USD", capturedRequest.getDescription());
    }
}
