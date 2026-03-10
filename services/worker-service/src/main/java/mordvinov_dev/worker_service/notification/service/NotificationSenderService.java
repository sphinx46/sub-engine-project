package mordvinov_dev.worker_service.notification.service;

import mordvinov_dev.worker_service.notification.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.notification.domain.dto.response.NotificationResult;

public interface NotificationSenderService {
    NotificationResult send(NotificationRequest request);
}
