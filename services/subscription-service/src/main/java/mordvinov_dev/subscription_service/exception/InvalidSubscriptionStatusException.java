package mordvinov_dev.subscription_service.exception;

import mordvinov_dev.subscription_service.entity.enums.StatusType;

import java.util.UUID;

public class InvalidSubscriptionStatusException extends RuntimeException {

    public InvalidSubscriptionStatusException(UUID subscriptionId, StatusType currentStatus, StatusType expectedStatus) {
        super(String.format("Subscription %s has status %s but expected %s",
                subscriptionId, currentStatus, expectedStatus));
    }

    public InvalidSubscriptionStatusException(String message) {
        super(message);
    }
}