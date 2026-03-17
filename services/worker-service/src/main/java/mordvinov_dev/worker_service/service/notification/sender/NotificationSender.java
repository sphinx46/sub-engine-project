package mordvinov_dev.worker_service.service.notification.sender;

import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;

/**
 * Interface for sending notifications through different channels.
 * Implementations should handle the specific logic for sending notifications
 * via their respective notification channels.
 */
public interface NotificationSender {
    /**
     * Sends a notification using the specific channel implementation.
     * 
     * @param request the notification request containing all necessary data
     * @return the response indicating the result of the send operation
     */
    NotificationResponse send(NotificationRequest request);
    
    /**
     * Returns the notification channel this sender handles.
     * 
     * @return the notification channel type
     */
    NotificationChannel getChannel();
}