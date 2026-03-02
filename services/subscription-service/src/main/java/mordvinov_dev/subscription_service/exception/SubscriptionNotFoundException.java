package mordvinov_dev.subscription_service.exception;

import java.util.UUID;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(UUID subscriptionId) {
        super("Subscription not found with id: " + subscriptionId);
    }

    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}