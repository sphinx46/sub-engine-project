package mordvinov_dev.worker_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.exception.NotificationException;
import mordvinov_dev.worker_service.service.notification.NotificationService;
import mordvinov_dev.worker_service.service.notification.PaymentEventProcessor;
import mordvinov_dev.worker_service.service.audit.AuditLogService;
import mordvinov_dev.worker_service.service.notification.sender.SenderRegistry;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProcessorImpl implements PaymentEventProcessor {

    private final AuditLogService auditService;
    private final NotificationService notificationService;
    private final SenderRegistry senderRegistry;

    @Override
    public void process(PaymentEvent event) {
        log.info("Processing event: eventId={}, status={}, userId={}, email={}",
                event.getEventId(), event.getStatus(), event.getUserId(), event.getUserEmail());

        try {
            if (auditService.existsByEventId(event.getEventId().toString())) {
                log.warn("Duplicate event: {}", event.getEventId());
                return;
            }

            auditService.createAuditLog(event);

            Notification notification = notificationService.createNotification(event);

            NotificationRequest request = buildRequest(event, notification);
            NotificationResponse response = senderRegistry.getSender(NotificationChannel.EMAIL).send(request);

            if (response.isSuccess()) {
                notificationService.markAsSent(notification);
                log.info("Event processed successfully: {}, email sent to: {}",
                        event.getEventId(), notification.getRecipient());
            } else {
                notificationService.markAsFailed(notification, response.getMessage());
                log.error("Event processing failed: {}, error: {}", event.getEventId(), response.getMessage());
                throw new NotificationException("Failed to send notification: " + response.getMessage());
            }

        } catch (NotificationException e) {
            log.error("Notification error processing event: {}, error: {}", event.getEventId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing event: {}, error: {}", event.getEventId(), e.getMessage(), e);
            throw new NotificationException("Failed to process payment event: " + e.getMessage(), e);
        }
    }

    private NotificationRequest buildRequest(PaymentEvent event, Notification notification) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("paymentId", event.getPaymentId());
            templateData.put("amount", event.getAmount());
            templateData.put("currency", event.getCurrency());
            templateData.put("status", event.getStatus());
            templateData.put("description", event.getDescription());
            templateData.put("userId", event.getUserId().toString());

            if (event.getSubscriptionId() != null) {
                templateData.put("subscriptionId", event.getSubscriptionId().toString());
            }

            if (event.getUserEmail() != null) {
                templateData.put("userEmail", event.getUserEmail());
            }

            if ("failed".equalsIgnoreCase(event.getStatus())) {
                templateData.put("reason", "Payment processing failed");
            }

            return NotificationRequest.builder()
                    .userId(event.getUserId())
                    .channel(NotificationChannel.EMAIL)
                    .recipient(notification.getRecipient())
                    .subject(notification.getSubject())
                    .templateName(getTemplateName(event.getStatus()))
                    .templateData(templateData)
                    .build();

        } catch (Exception e) {
            log.error("Failed to build notification request for event: {}", event.getEventId(), e);
            throw new NotificationException("Failed to build notification request: " + e.getMessage(), e);
        }
    }

    private String getTemplateName(String status) {
        try {
            return switch (status.toLowerCase()) {
                case "succeeded" -> "payment-success";
                case "failed" -> "payment-failed";
                case "canceled" -> "payment-canceled";
                case "waiting_for_capture" -> "payment-pending";
                default -> "default";
            };
        } catch (Exception e) {
            log.warn("Failed to get template name for status: {}, using default", status);
            return "default";
        }
    }
}