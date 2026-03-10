package mordvinov_dev.worker_service.service.notification.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import mordvinov_dev.worker_service.domain.NotificationType;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.service.notification.EmailNotificationService;
import mordvinov_dev.worker_service.service.notification.NotificationPersistenceService;
import mordvinov_dev.worker_service.service.notification.NotificationSenderService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final NotificationSenderService notificationSenderService;
    private final NotificationPersistenceService notificationPersistenceService;

    @Override
    public NotificationResult sendEmailNotification(PaymentEvent event, Notification notification) {
        log.debug("Preparing email notification for event: {}", event.getEventId());

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("paymentId", event.getPaymentId());
        templateData.put("amount", event.getAmount());
        templateData.put("currency", event.getCurrency());
        templateData.put("status", event.getStatus());
        templateData.put("description", event.getDescription() != null ? event.getDescription() : "—");

        if ("failed".equalsIgnoreCase(event.getStatus())) {
            templateData.put("reason", "Payment processing failed");
        }

        NotificationRequest request = NotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.EMAIL)
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .templateName(getTemplateNameForStatus(event.getStatus()))
                .templateData(templateData)
                .build();

        try {
            NotificationResult sendResult = notificationSenderService.send(request);

            if (sendResult.isSuccess()) {
                NotificationResult result = notificationPersistenceService.markAsSent(notification);
                log.info("Email notification sent successfully: eventId={}, resultId={}",
                        event.getEventId(), result.getNotificationId());
                return result;
            } else {
                log.error("Failed to send email notification: eventId={}, error={}",
                        event.getEventId(), sendResult.getMessage());
                return notificationPersistenceService.markAsFailed(notification, sendResult.getMessage());
            }
        } catch (Exception e) {
            log.error("Error sending email notification: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            return notificationPersistenceService.markAsFailed(notification, e.getMessage());
        }
    }

    private String getTemplateNameForStatus(String status) {
        switch (status.toLowerCase()) {
            case "succeeded":
                return "payment-success";
            case "failed":
                return "payment-failed";
            case "canceled":
                return "payment-canceled";
            case "waiting_for_capture":
                return "payment-pending";
            default:
                return "default";
        }
    }
}