package mordvinov_dev.worker_service.service.notification.strategy.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import mordvinov_dev.worker_service.domain.NotificationType;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import mordvinov_dev.worker_service.exception.NotificationSendException;
import mordvinov_dev.worker_service.service.notification.strategy.NotificationStrategy;
import mordvinov_dev.worker_service.service.notification.strategy.NotificationTemplateProcessor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationStrategy {

    private final JavaMailSender mailSender;
    private final NotificationTemplateProcessor templateProcessor;

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info("Sending email notification to: {}, subject: {}", request.getRecipient(), request.getSubject());

        String notificationId = UUID.randomUUID().toString();

        try {
            String content = resolveContent(request);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getRecipient());
            message.setSubject(request.getSubject());
            message.setText(content);

            mailSender.send(message);

            log.info("Email sent successfully to: {}, notificationId: {}", request.getRecipient(), notificationId);

            return NotificationResult.builder()
                    .notificationId(notificationId)
                    .success(true)
                    .message("Email sent successfully")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (MailException e) {
            log.error("Failed to send email to: {}, notificationId: {}, error: {}",
                    request.getRecipient(), notificationId, e.getMessage(), e);

            throw new NotificationSendException(
                    String.format("Failed to send email to %s: %s", request.getRecipient(), e.getMessage()),
                    e
            );
        }
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    private String resolveContent(NotificationRequest request) {
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            return request.getContent();
        }

        if (request.getTemplateName() != null && request.getTemplateData() != null) {
            return templateProcessor.process(request.getTemplateName(), request.getTemplateData());
        }

        throw new IllegalArgumentException("Either content or templateName with templateData must be provided");
    }
}