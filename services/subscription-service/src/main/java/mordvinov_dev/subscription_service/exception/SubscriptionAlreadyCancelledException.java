package mordvinov_dev.subscription_service.exception;

import java.util.UUID;

public class SubscriptionAlreadyCancelledException extends RuntimeException {

    public SubscriptionAlreadyCancelledException(UUID subscriptionId) {
        super("Subscription is already cancelled: " + subscriptionId);
    }
}