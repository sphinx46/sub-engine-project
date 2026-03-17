package mordvinov_dev.billing_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.event.PaymentEvent;
import mordvinov_dev.billing_service.exception.PaymentNotFoundException;
import mordvinov_dev.billing_service.producer.PaymentEventProducer;
import mordvinov_dev.billing_service.producer.PremiumSubscriptionProducer;
import mordvinov_dev.billing_service.repository.PaymentRepository;
import mordvinov_dev.billing_service.util.WebhookTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private PremiumSubscriptionProducer premiumSubscriptionProducer;

    @InjectMocks
    private WebhookServiceImpl webhookService;

    private PaymentEntity paymentEntity;
    private PaymentEntity paymentEntityWithoutSubscription;
    private PaymentEntity paymentEntityWithoutEmail;

    @BeforeEach
    void setUp() {
        paymentEntity = WebhookTestDataFactory.createTestPaymentEntity();
        paymentEntityWithoutSubscription = WebhookTestDataFactory.createTestPaymentEntityWithoutSubscription();
        paymentEntityWithoutEmail = WebhookTestDataFactory.createTestPaymentEntityWithoutEmail();
    }

    @Test
    void processWebhook_SuccessfulPayment_WithSubscription() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("succeeded", paymentEntity.getStatus());
        verify(premiumSubscriptionProducer).sendPaymentSuccessResponse(
                WebhookTestDataFactory.TEST_SUBSCRIPTION_ID,
                WebhookTestDataFactory.TEST_USER_ID,
                WebhookTestDataFactory.TEST_PAYMENT_ID
        );
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_SuccessfulPayment_WithoutSubscription() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutSubscription));

        webhookService.processWebhook(webhook);

        assertEquals("succeeded", paymentEntityWithoutSubscription.getStatus());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_CanceledPayment() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentCanceledWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("canceled", paymentEntity.getStatus());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_WaitingForCapturePayment() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentWaitingForCaptureWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("waiting_for_capture", paymentEntity.getStatus());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_UnknownEvent() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentFailedWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("pending", paymentEntity.getStatus());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_PaymentNotFound() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            webhookService.processWebhook(webhook);
        });

        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_MissingPaymentId() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithoutPaymentId();

        assertThrows(NullPointerException.class, () -> {
            webhookService.processWebhook(webhook);
        });

        verify(paymentRepository, never()).findByPaymentId(any());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_MissingObject() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithoutObject();

        assertDoesNotThrow(() -> {
            webhookService.processWebhook(webhook);
        });

        verify(paymentRepository, never()).findByPaymentId(any());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_MissingEvent() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithoutEvent();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("pending", paymentEntity.getStatus());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_EmailExtractionFromPaymentEntity() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(eventCaptor.capture());
        
        PaymentEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(WebhookTestDataFactory.TEST_PAYMENT_ID, capturedEvent.getPaymentId());
        assertEquals(WebhookTestDataFactory.TEST_USER_ID, capturedEvent.getUserId());
    }

    @Test
    void processWebhook_EmailExtractionFromMetadata() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutEmail));

        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_EmailExtractionFails() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithMalformedJson();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutEmail));

        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_EmptyMetadata() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithEmptyMetadata();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutEmail));

        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_NullMetadata() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithNullMetadata();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutEmail));

        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_NoStatusChange() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        paymentEntity.setStatus("succeeded");
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("succeeded", paymentEntity.getStatus());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_RepositoryThrowsException() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            webhookService.processWebhook(webhook);
        });

        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_ProducerThrowsException() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));
        doThrow(new RuntimeException("Kafka error")).when(paymentEventProducer).sendPaymentEvent(any());

        assertThrows(RuntimeException.class, () -> {
            webhookService.processWebhook(webhook);
        });
    }

    @Test
    void processWebhook_PremiumSubscriptionProducerThrowsException() {
        JsonNode webhook = WebhookTestDataFactory.createPaymentSucceededWebhook();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));
        doThrow(new RuntimeException("Kafka error")).when(premiumSubscriptionProducer).sendPaymentSuccessResponse(any(), any(), any());

        assertThrows(RuntimeException.class, () -> {
            webhookService.processWebhook(webhook);
        });
    }

    @Test
    void processWebhook_NullPayload() {
        assertThrows(NullPointerException.class, () -> {
            webhookService.processWebhook(null);
        });

        verify(paymentRepository, never()).findByPaymentId(any());
        verify(premiumSubscriptionProducer, never()).sendPaymentSuccessResponse(any(), any(), any());
        verify(paymentEventProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_EmptyEventField() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithEmptyEventField();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntity));

        webhookService.processWebhook(webhook);

        assertEquals("pending", paymentEntity.getStatus());
        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_SpecialCharactersInEmail() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithSpecialCharactersInEmail();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutEmail));

        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(any());
    }

    @Test
    void processWebhook_VeryLongEmail() {
        JsonNode webhook = WebhookTestDataFactory.createWebhookWithVeryLongEmail();
        when(paymentRepository.findByPaymentId(WebhookTestDataFactory.TEST_PAYMENT_ID))
                .thenReturn(Optional.of(paymentEntityWithoutEmail));

        webhookService.processWebhook(webhook);

        verify(paymentEventProducer).sendPaymentEvent(any());
    }
}
