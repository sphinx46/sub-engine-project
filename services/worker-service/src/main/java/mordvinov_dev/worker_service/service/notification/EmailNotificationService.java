package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import mordvinov_dev.worker_service.event.PaymentEvent;

public interface EmailNotificationService {
    NotificationResult sendEmailNotification(PaymentEvent event, Notification notification);
}