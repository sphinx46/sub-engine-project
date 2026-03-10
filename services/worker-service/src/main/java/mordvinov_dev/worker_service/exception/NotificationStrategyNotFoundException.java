package mordvinov_dev.worker_service.exception;

import mordvinov_dev.worker_service.domain.NotificationType;

public class NotificationStrategyNotFoundException extends NotificationException {

    public NotificationStrategyNotFoundException(NotificationType type) {
        super(String.format("Notification strategy not found for type: %s", type));
    }
}