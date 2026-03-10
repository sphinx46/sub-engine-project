package mordvinov_dev.worker_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.repository.NotificationRepository;
import mordvinov_dev.worker_service.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Value("${notification.email.recipient:user@example.com}")
    private String defaultEmailRecipient;

    @Override
    public Notification createNotification(PaymentEvent event) {
        String type = "PAYMENT_" + event.getStatus().toUpperCase();

        String recipient = event.getUserEmail() != null && !event.getUserEmail().isEmpty()
                ? event.getUserEmail()
                : defaultEmailRecipient;

        if (event.getUserEmail() == null || event.getUserEmail().isEmpty()) {
            log.warn("User email not found in PaymentEvent for userId: {}, using default recipient: {}",
                    event.getUserId(), defaultEmailRecipient);
        } else {
            log.debug("Using user email for notification: {}", recipient);
        }

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(type)
                .channel(NotificationChannel.EMAIL.name())
                .recipient(recipient)
                .subject(getSubject(event.getStatus()))
                .content(buildContent(event))
                .sent(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, userId={}, type={}, recipient={}",
                saved.getId(), event.getUserId(), type, recipient);

        return saved;
    }

    @Override
    public NotificationResponse markAsSent(Notification notification) {
        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now());

        Notification updated = notificationRepository.save(notification);
        log.debug("Notification marked as sent: id={}, recipient={}", updated.getId(), updated.getRecipient());

        return NotificationResponse.builder()
                .notificationId(updated.getId())
                .success(true)
                .message("Notification sent successfully")
                .timestamp(updated.getSentAt())
                .build();
    }

    @Override
    public NotificationResponse markAsFailed(Notification notification, String error) {
        notification.setSent(false);
        notification.setSentAt(LocalDateTime.now());

        Notification updated = notificationRepository.save(notification);
        log.debug("Notification marked as failed: id={}, recipient={}, error={}",
                updated.getId(), updated.getRecipient(), error);

        return NotificationResponse.builder()
                .notificationId(updated.getId())
                .success(false)
                .message("Failed: " + error)
                .timestamp(updated.getSentAt())
                .build();
    }

    private String getSubject(String status) {
        switch (status.toLowerCase()) {
            case "succeeded": return "Платеж успешно выполнен";
            case "canceled": return "Платеж отменен";
            case "failed": return "Платеж не удался";
            case "waiting_for_capture": return "Платеж ожидает подтверждения";
            default: return "Обновление статуса платежа";
        }
    }

    private String buildContent(PaymentEvent event) {
        String statusText = switch (event.getStatus().toLowerCase()) {
            case "succeeded" -> "успешно выполнен";
            case "waiting_for_capture" -> "ожидает подтверждения";
            case "canceled" -> "отменен";
            case "failed" -> "не удался";
            default -> event.getStatus();
        };

        return String.format("Платеж на сумму %.2f %s %s. %s",
                event.getAmount(), event.getCurrency(), statusText,
                event.getDescription() != null ? "Описание: " + event.getDescription() : "");
    }
}