package mordvinov_dev.subscription_service.exception;

import java.util.UUID;

public class SubscriptionAccessDeniedException extends RuntimeException {

    public SubscriptionAccessDeniedException(UUID subscriptionId, UUID userId) {
        super(String.format("User %s does not have access to subscription %s", userId, subscriptionId));
    }

    public SubscriptionAccessDeniedException(String message) {
        super(message);
    }
}