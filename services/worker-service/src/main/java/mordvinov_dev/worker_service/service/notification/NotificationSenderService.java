package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;

public interface NotificationSenderService {
    NotificationResult send(NotificationRequest request);
}
