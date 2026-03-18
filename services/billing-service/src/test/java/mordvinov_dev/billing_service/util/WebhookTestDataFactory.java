package mordvinov_dev.billing_service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mordvinov_dev.billing_service.entity.PaymentEntity;

import java.math.BigDecimal;
import java.util.UUID;

public class WebhookTestDataFactory {

    public static final String TEST_PAYMENT_ID = "test-payment-id";
    public static final UUID TEST_SUBSCRIPTION_ID = UUID.randomUUID();
    public static final UUID TEST_USER_ID = UUID.randomUUID();
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String DEFAULT_CURRENCY = "RUB";
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("1000.00");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static PaymentEntity createTestPaymentEntity() {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(TEST_SUBSCRIPTION_ID)
                .status("pending")
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static PaymentEntity createTestPaymentEntityWithoutSubscription() {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(null)
                .status("pending")
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment without subscription")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static PaymentEntity createTestPaymentEntityWithoutEmail() {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(TEST_USER_ID)
                .subscriptionId(TEST_SUBSCRIPTION_ID)
                .status("pending")
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment without email")
                .userEmail(null)
                .build();
    }

    public static JsonNode createPaymentSucceededWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createPaymentCanceledWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.canceled");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "canceled");
        objectNode.put("paid", false);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createPaymentWaitingForCaptureWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.waiting_for_capture");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "waiting_for_capture");
        objectNode.put("paid", false);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createPaymentFailedWebhook() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.failed");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "failed");
        objectNode.put("paid", false);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithoutPaymentId() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithoutObject() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        return webhook;
    }

    public static JsonNode createWebhookWithoutEvent() {
        ObjectNode webhook = objectMapper.createObjectNode();
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithMalformedJson() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", "invalid-email-json");
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithEmptyMetadata() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithNullMetadata() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        objectNode.putNull("metadata");
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithEmptyEventField() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", TEST_USER_EMAIL);
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithSpecialCharactersInEmail() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", "test+special@example.com");
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }

    public static JsonNode createWebhookWithVeryLongEmail() {
        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("event", "payment.succeeded");
        
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", TEST_PAYMENT_ID);
        objectNode.put("status", "succeeded");
        objectNode.put("paid", true);
        
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("userEmail", "very.long.email.address.that.exceeds.normal.length.limits@example.com");
        objectNode.set("metadata", metadata);
        
        webhook.set("object", objectNode);
        return webhook;
    }
}
