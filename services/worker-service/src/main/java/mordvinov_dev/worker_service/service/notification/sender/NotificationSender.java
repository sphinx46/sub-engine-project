package mordvinov_dev.worker_service.service.notification.sender;

import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;

public interface NotificationSender {
    NotificationResponse send(NotificationRequest request);
    NotificationChannel getChannel();
}