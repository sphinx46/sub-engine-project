package mordvinov_dev.subscription_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.event.PremiumSubscriptionResponseEvent;
import mordvinov_dev.subscription_service.service.impl.SubscriptionServiceImpl;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionResponseListener {

    private final SubscriptionServiceImpl subscriptionService;

    @KafkaListener(
            topics = "${kafka.topics.premium-subscription-response:premium-subscription-response}",
            groupId = "${spring.kafka.consumer.group-id:subscription-service-group}",
            containerFactory = "premiumSubscriptionResponseKafkaListenerContainerFactory"
    )
    @Transactional
    public void onPremiumSubscriptionResponse(@Payload final PremiumSubscriptionResponseEvent event) {
        log.info("Received premium subscription response, eventId={}, userId={}, subscriptionId={}, status={}, paymentId={}",
                event.getEventId(), event.getUserId(), event.getSubscriptionId(),
                event.getStatus(), event.getPaymentId());

        try {
            if ("SUCCESS".equals(event.getStatus())) {
                log.info("Payment succeeded for subscription {}, activating subscription", event.getSubscriptionId());
                subscriptionService.activatePremiumSubscription(event.getSubscriptionId());
            } else if ("FAILED".equals(event.getStatus())) {
                log.error("Payment failed for subscription {}, message={}",
                        event.getSubscriptionId(), event.getMessage());
                subscriptionService.failPremiumSubscription(event.getSubscriptionId());
            } else {
                log.info("Payment pending for subscription {}, paymentId={}",
                        event.getSubscriptionId(), event.getPaymentId());
            }
        } catch (Exception e) {
            log.error("Error processing premium subscription response, subscriptionId={}, error={}",
                    event.getSubscriptionId(), e.getMessage(), e);
        }
    }
}