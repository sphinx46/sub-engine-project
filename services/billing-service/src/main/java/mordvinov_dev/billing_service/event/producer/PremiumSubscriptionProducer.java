package mordvinov_dev.billing_service.event.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.event.PremiumSubscriptionResponseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for premium subscription response events.
 * Sends payment success notifications to the premium subscription service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionProducer {

    private final KafkaTemplate<String, PremiumSubscriptionResponseEvent> premiumSubscriptionResponseKafkaTemplate;
    private static final String RESPONSE_TOPIC = "premium-subscription-response";

    /**
     * Sends a payment success response event.
     * @param subscriptionId subscription identifier
     * @param userId user identifier
     * @param paymentId payment identifier
     */
    public void sendPaymentSuccessResponse(UUID subscriptionId, UUID userId, String paymentId) {
        log.info("Sending payment success response, subscriptionId={}, userId={}, paymentId={}",
                subscriptionId, userId, paymentId);

        PremiumSubscriptionResponseEvent event = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId(paymentId)
                .status("SUCCESS")
                .message("Payment completed successfully")
                .build();

        sendResponse(event, userId.toString() + "_" + event.getEventId());
    }

    /**
     * Sends a response event to Kafka.
     * @param event the response event to send
     * @param key the Kafka message key
     */
    private void sendResponse(PremiumSubscriptionResponseEvent event, String key) {
        try {
            CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future =
                    premiumSubscriptionResponseKafkaTemplate.send(RESPONSE_TOPIC, key, event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send response, subscriptionId={}, userId={}, error={}",
                            event.getSubscriptionId(), event.getUserId(), exception.getMessage());
                } else {
                    log.info("Response sent successfully, subscriptionId={}, userId={}, status={}, partition={}, offset={}",
                            event.getSubscriptionId(), event.getUserId(), event.getStatus(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Critical error sending response, subscriptionId={}, userId={}",
                    event.getSubscriptionId(), event.getUserId(), e);
            throw new RuntimeException("Failed to send premium subscription response", e);
        }
    }
}