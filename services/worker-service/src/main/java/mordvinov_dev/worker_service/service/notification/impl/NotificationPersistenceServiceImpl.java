package mordvinov_dev.worker_service.service.notification.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import mordvinov_dev.worker_service.domain.NotificationType;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.mapping.EntityMapper;
import mordvinov_dev.worker_service.repository.NotificationRepository;
import mordvinov_dev.worker_service.service.notification.NotificationPersistenceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceServiceImpl implements NotificationPersistenceService {

    private final NotificationRepository notificationRepository;
    private final EntityMapper entityMapper;

    @Value("${notification.email.recipient:user@example.com}")
    private String defaultEmailRecipient;

    @Override
    public Notification createNotification(PaymentEvent event) {
        log.debug("Creating notification for event: {}, status: {}", event.getEventId(), event.getStatus());

        String notificationType = "PAYMENT_" + event.getStatus().toUpperCase();

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(notificationType)
                .channel(NotificationType.EMAIL.name())
                .recipient(defaultEmailRecipient)
                .subject(getSubjectForStatus(event.getStatus()))
                .content(createNotificationContent(event))
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, userId={}, type={}", saved.getId(), event.getUserId(), notificationType);

        return saved;
    }

    @Override
    public NotificationResult markAsSent(Notification notification) {
        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now());

        Notification updated = notificationRepository.save(notification);
        log.debug("Notification marked as sent: id={}", updated.getId());

        return entityMapper.map(updated, NotificationResult.class);
    }

    @Override
    public NotificationResult markAsFailed(Notification notification, String errorMessage) {
        notification.setSent(false);
        notification.setSentAt(LocalDateTime.now());

        Notification updated = notificationRepository.save(notification);
        log.debug("Notification marked as failed: id={}, error={}", updated.getId(), errorMessage);

        NotificationResult result = entityMapper.map(updated, NotificationResult.class);

        return NotificationResult.builder()
                .notificationId(result.getNotificationId())
                .success(false)
                .message("Failed to send notification: " + errorMessage)
                .timestamp(result.getTimestamp())
                .build();
    }

    private String getSubjectForStatus(String status) {
        switch (status.toLowerCase()) {
            case "succeeded":
                return "Платеж успешно выполнен";
            case "canceled":
                return "Платеж отменен";
            case "failed":
                return "Платеж не удался";
            case "waiting_for_capture":
                return "Платеж ожидает подтверждения";
            default:
                return "Обновление статуса платежа";
        }
    }

    private String createNotificationContent(PaymentEvent event) {
        String statusText;
        switch (event.getStatus().toLowerCase()) {
            case "succeeded":
                statusText = "успешно выполнен";
                break;
            case "waiting_for_capture":
                statusText = "ожидает подтверждения";
                break;
            case "canceled":
                statusText = "отменен";
                break;
            case "failed":
                statusText = "не удался";
                break;
            default:
                statusText = event.getStatus();
        }

        return String.format(
                "Платеж на сумму %.2f %s %s. %s",
                event.getAmount(),
                event.getCurrency(),
                statusText,
                event.getDescription() != null ? "Описание: " + event.getDescription() : ""
        );
    }
}