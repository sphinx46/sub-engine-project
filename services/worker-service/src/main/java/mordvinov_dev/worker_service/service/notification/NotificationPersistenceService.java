package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.document.Notification;
import mordvinov_dev.worker_service.event.PaymentEvent;

public interface NotificationPersistenceService {
    Notification createNotification(PaymentEvent event);
    Notification markAsSent(Notification notification, String resultId);
}