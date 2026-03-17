package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.event.PaymentEvent;

/**
 * Service interface for managing notifications related to payment events.
 * Provides methods for creating, updating, and tracking notification status.
 */
public interface NotificationService {
    /**
     * Creates a new notification based on a payment event.
     * 
     * @param event the payment event to create a notification for
     * @return the created notification
     */
    Notification createNotification(PaymentEvent event);
    
    /**
     * Marks a notification as successfully sent.
     * 
     * @param notification the notification to mark as sent
     * @return the notification response with updated status
     */
    NotificationResponse markAsSent(Notification notification);
    
    /**
     * Marks a notification as failed due to an error.
     * 
     * @param notification the notification to mark as failed
     * @param error the error message describing the failure
     * @return the notification response with updated status and error information
     */
    NotificationResponse markAsFailed(Notification notification, String error);
}