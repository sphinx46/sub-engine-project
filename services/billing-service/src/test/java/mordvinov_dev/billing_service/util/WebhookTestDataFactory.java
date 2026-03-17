package mordvinov_dev.billing_service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.event.PaymentEvent;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebhookTestDataFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String TEST_PAYMENT_ID = "test-payment-id-123";
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final UUID TEST_USER_ID = UUID.randomUUID();
    public static final UUID TEST_SUBSCRIPTION_ID = UUID.randomUUID();

    public static JsonNode createPaymentSucceededWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        paymentObject.put("amount", "1000.00");
        paymentObject.put("currency", "RUB");
        paymentObject.put("description", "Test payment");
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        paymentObject.set("metadata", metadata);
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createPaymentCanceledWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.canceled");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "canceled");
        paymentObject.put("amount", "1000.00");
        paymentObject.put("currency", "RUB");
        paymentObject.put("description", "Test payment");
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        paymentObject.set("metadata", metadata);
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createPaymentWaitingForCaptureWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.waiting_for_capture");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "waiting_for_capture");
        paymentObject.put("amount", "1000.00");
        paymentObject.put("currency", "RUB");
        paymentObject.put("description", "Test payment");
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createPaymentFailedWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.failed");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "failed");
        paymentObject.put("amount", "1000.00");
        paymentObject.put("currency", "RUB");
        paymentObject.put("description", "Test payment");
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithoutEvent() {
        ObjectNode webhook = objectMapper.createObjectNode();
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithoutObject() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        return webhook;
    }

    public static JsonNode createWebhookWithoutPaymentId() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("status", "succeeded");
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithInvalidEvent() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.unknown_event");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "unknown");
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithMalformedJson() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        paymentObject.set("metadata", metadata);
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithEmptyMetadata() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        
        ObjectNode metadata = objectMapper.createObjectNode();
        paymentObject.set("metadata", metadata);
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithNullMetadata() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        paymentObject.putNull("metadata");
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static PaymentEntity createTestPaymentEntity() {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(TEST_SUBSCRIPTION_ID)
                .status("pending")
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Test payment")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static PaymentEntity createTestPaymentEntityWithoutEmail() {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(TEST_SUBSCRIPTION_ID)
                .status("pending")
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Test payment")
                .userEmail(null)
                .build();
    }

    public static PaymentEntity createTestPaymentEntityWithoutSubscription() {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(null)
                .status("pending")
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Test payment")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static PaymentEvent createExpectedPaymentEvent(String eventType, String status) {
        return PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_" + eventType.toUpperCase().replace(".", "_"))
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(TEST_SUBSCRIPTION_ID)
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .status(status)
                .description("Test payment")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static JsonNode createWebhookWithSpecialCharactersInEmail() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", "test+tag@example.com");
        paymentObject.set("metadata", metadata);
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithVeryLongEmail() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", "very.long.email.address.that.exceeds.normal.length.limits@example.com");
        paymentObject.set("metadata", metadata);
        
        webhook.set("object", paymentObject);
        return webhook;
    }

    public static JsonNode createWebhookWithEmptyEventField() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "");
        
        ObjectNode paymentObject = objectMapper.createObjectNode();
        paymentObject.put("id", TEST_PAYMENT_ID);
        paymentObject.put("status", "succeeded");
        
        webhook.set("object", paymentObject);
        return webhook;
    }
}
