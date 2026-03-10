package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.event.PaymentEvent;

public interface NotificationService {
    Notification createNotification(PaymentEvent event);
    NotificationResponse markAsSent(Notification notification);
    NotificationResponse markAsFailed(Notification notification, String error);
}