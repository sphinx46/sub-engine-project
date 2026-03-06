package mordvinov_dev.subscription_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.event.PremiumSubscriptionRequestEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionProducer {

    private final KafkaTemplate<String, PremiumSubscriptionRequestEvent> premiumSubscriptionRequestKafkaTemplate;
    private static final String REQUEST_TOPIC = "premium-subscription-request";

    public void sendPremiumSubscriptionRequest(Subscription subscription, UUID userId) {
        log.info("Sending premium subscription request, subscriptionId={}, userId={}", subscription.getId(), userId);

        PremiumSubscriptionRequestEvent event = PremiumSubscriptionRequestEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_REQUEST")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscription.getId())
                .userId(userId)
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .description("Premium subscription payment")
                .build();

        String key = userId.toString() + "_" + event.getEventId();

        try {
            CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future =
                    premiumSubscriptionRequestKafkaTemplate.send(REQUEST_TOPIC, key, event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send premium subscription request, subscriptionId={}, userId={}, error={}",
                            subscription.getId(), userId, exception.getMessage());
                } else {
                    log.info("Premium subscription request sent successfully, subscriptionId={}, userId={}, partition={}, offset={}",
                            subscription.getId(), userId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Critical error sending premium subscription request, subscriptionId={}, userId={}",
                    subscription.getId(), userId, e);
            throw new RuntimeException("Failed to send premium subscription request", e);
        }
    }
}