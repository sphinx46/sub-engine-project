package mordvinov_dev.worker_service.notification.strategy;


import mordvinov_dev.worker_service.notification.domain.NotificationType;
import mordvinov_dev.worker_service.notification.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.notification.domain.dto.response.NotificationResult;

public interface NotificationStrategy {
    NotificationResult send(NotificationRequest request);
    NotificationType getType();
}