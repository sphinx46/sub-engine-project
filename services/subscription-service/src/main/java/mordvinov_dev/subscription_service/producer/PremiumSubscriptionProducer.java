package mordvinov_dev.subscription_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.event.PremiumSubscriptionRequestEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Producer for sending premium subscription request events to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionProducer {

    private final KafkaTemplate<String, PremiumSubscriptionRequestEvent> premiumSubscriptionRequestKafkaTemplate;

    @Value("${kafka.topics.premium-subscription-request:premium-subscription-request}")
    private String requestTopic;

    /**
     * Sends a premium subscription request event to Kafka.
     * @param subscription the subscription entity
     * @param userId the unique identifier of the user
     * @param userEmail the email address of the user
     */
    public void sendPremiumSubscriptionRequest(Subscription subscription, UUID userId, String userEmail) {
        log.info("Sending premium subscription request for subscription: {}, user: {}",
                subscription.getId(), userId);

        PremiumSubscriptionRequestEvent event = PremiumSubscriptionRequestEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscription.getId())
                .userId(userId)
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Premium subscription payment")
                .userEmail(userEmail)
                .build();

        sendRequest(event, userId.toString() + "_" + event.getEventId());
    }

    /**
     * Sends the premium subscription request event to Kafka topic.
     * @param event the premium subscription request event
     * @param key the message key for partitioning
     */
    private void sendRequest(PremiumSubscriptionRequestEvent event, String key) {
        try {
            CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future =
                    premiumSubscriptionRequestKafkaTemplate.send(requestTopic, key, event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send premium subscription request, subscriptionId={}, userId={}, error={}",
                            event.getSubscriptionId(), event.getUserId(), exception.getMessage());
                } else {
                    log.info("Premium subscription request sent successfully, subscriptionId={}, userId={}, partition={}, offset={}",
                            event.getSubscriptionId(), event.getUserId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Critical error sending premium subscription request, subscriptionId={}, userId={}",
                    event.getSubscriptionId(), event.getUserId(), e);
            throw new RuntimeException("Failed to send premium subscription request", e);
        }
    }
}