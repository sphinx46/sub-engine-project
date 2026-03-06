package mordvinov_dev.subscription_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;
import mordvinov_dev.subscription_service.event.PremiumSubscriptionResponseEvent;
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionResponseListener {

    private final SubscriptionRepository subscriptionRepository;

    @KafkaListener(
            topics = "${kafka.topics.premium-subscription-response:premium-subscription-response}",
            groupId = "${spring.kafka.consumer.group-id:subscription-service-group}",
            containerFactory = "premiumSubscriptionResponseKafkaListenerContainerFactory"
    )
    public void onPremiumSubscriptionResponse(@Payload final PremiumSubscriptionResponseEvent event) {
        log.info("Received premium subscription response, eventId={}, userId={}, subscriptionId={}, status={}, paymentId={}",
                event.getEventId(), event.getUserId(), event.getSubscriptionId(),
                event.getStatus(), event.getPaymentId());

        try {
            if ("PENDING".equals(event.getStatus())) {
                log.info("Payment pending for subscription {}, paymentId={}, confirmationUrl={}",
                        event.getSubscriptionId(), event.getPaymentId(), event.getConfirmationUrl());
                return;
            }

            if ("FAILED".equals(event.getStatus())) {
                log.error("Payment failed for subscription {}, message={}",
                        event.getSubscriptionId(), event.getMessage());
                return;
            }

            Subscription subscription = subscriptionRepository.findById(event.getSubscriptionId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + event.getSubscriptionId()));

            if (!subscription.getUserId().equals(event.getUserId())) {
                log.error("User {} does not own subscription {}", event.getUserId(), event.getSubscriptionId());
                return;
            }

            subscription.setPlanType(PlanType.PREMIUM);
            subscription.setStatus(StatusType.ACTIVE);
            subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);

            log.info("Premium subscription activated, subscriptionId={}, userId={}, paymentId={}",
                    event.getSubscriptionId(), event.getUserId(), event.getPaymentId());

        } catch (Exception e) {
            log.error("Error processing premium subscription response, subscriptionId={}, error={}",
                    event.getSubscriptionId(), e.getMessage(), e);
        }
    }
}