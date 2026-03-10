package mordvinov_dev.worker_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.AuditLog;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.service.audit.AuditLogService;
import mordvinov_dev.worker_service.service.notification.EmailNotificationService;
import mordvinov_dev.worker_service.service.notification.NotificationPersistenceService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final AuditLogService auditLogService;
    private final NotificationPersistenceService notificationPersistenceService;
    private final EmailNotificationService emailNotificationService;

    @KafkaListener(
            topics = "${kafka.topics.payment-events:payment.events}",
            groupId = "${spring.kafka.consumer.group-id:worker-service-group}",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentEvent(@Payload PaymentEvent event) {
        log.info("Received payment event: eventId={}, paymentId={}, userId={}, status={}",
                event.getEventId(), event.getPaymentId(), event.getUserId(), event.getStatus());

        try {
            if (auditLogService.existsByEventId(event.getEventId().toString())) {
                log.warn("Duplicate event detected, skipping processing. eventId={}", event.getEventId());
                return;
            }

            AuditLog auditLog = auditLogService.createAuditLog(event);

            Notification notification = notificationPersistenceService.createNotification(event);

            NotificationResult result = emailNotificationService.sendEmailNotification(event, notification);

            log.info("Successfully processed payment event: eventId={}, auditLogId={}, notificationId={}, result={}",
                    event.getEventId(), auditLog.getId(), notification.getId(), result.isSuccess());

        } catch (Exception e) {
            log.error("Failed to process payment event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process payment event: " + e.getMessage(), e);
        }
    }
}