package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.document.Notification;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;

public interface EmailNotificationService {
    NotificationResult sendEmailNotification(PaymentEvent event, Notification notification);
}