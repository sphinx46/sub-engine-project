package mordvinov_dev.worker_service.service.notification.strategy;


import mordvinov_dev.worker_service.domain.NotificationType;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;

public interface NotificationStrategy {
    NotificationResult send(NotificationRequest request);
    NotificationType getType();
}