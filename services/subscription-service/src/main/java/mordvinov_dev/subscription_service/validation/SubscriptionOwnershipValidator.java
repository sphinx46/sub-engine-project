package mordvinov_dev.subscription_service.validation;

import lombok.RequiredArgsConstructor;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.exception.SubscriptionAccessDeniedException;
import mordvinov_dev.subscription_service.exception.SubscriptionNotFoundException;
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionOwnershipValidator {

    private final SubscriptionRepository subscriptionRepository;

    public Subscription validateAndGetSubscription(UUID subscriptionId, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        if (!subscription.getUserId().equals(userId)) {
            throw new SubscriptionAccessDeniedException(subscriptionId, userId);
        }

        return subscription;
    }
}