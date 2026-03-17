package mordvinov_dev.subscription_service.validation;

import lombok.RequiredArgsConstructor;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.exception.SubscriptionAccessDeniedException;
import mordvinov_dev.subscription_service.exception.SubscriptionNotFoundException;
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validator for subscription ownership and access rights.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionOwnershipValidator {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Validates that the subscription exists and belongs to the specified user.
     * @param subscriptionId the unique identifier of the subscription
     * @param userId the unique identifier of the user
     * @return the validated subscription entity
     * @throws SubscriptionNotFoundException if subscription is not found
     * @throws SubscriptionAccessDeniedException if user does not own the subscription
     */
    public Subscription validateAndGetSubscription(UUID subscriptionId, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        if (!subscription.getUserId().equals(userId)) {
            throw new SubscriptionAccessDeniedException(subscriptionId, userId);
        }

        return subscription;
    }
}