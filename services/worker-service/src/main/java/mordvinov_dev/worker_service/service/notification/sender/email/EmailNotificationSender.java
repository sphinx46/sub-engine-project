package mordvinov_dev.worker_service.service.notification.sender.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.service.notification.NotificationTemplateProcessor;
import mordvinov_dev.worker_service.service.notification.sender.NotificationSender;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Email notification sender implementation.
 * Handles sending email notifications using Spring's JavaMailSender and template processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationTemplateProcessor templateProcessor;

    /** {@inheritDoc} */
    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.info("Sending email to: {}, subject: {}", request.getRecipient(), request.getSubject());

        String notificationId = UUID.randomUUID().toString();

        try {
            String content = resolveContent(request);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getRecipient());
            message.setSubject(request.getSubject());
            message.setText(content);

            mailSender.send(message);

            log.info("Email sent successfully: {}", notificationId);

            return NotificationResponse.builder()
                    .notificationId(notificationId)
                    .success(true)
                    .message("Email sent successfully")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (MailException e) {
            log.error("Failed to send email: {}, error: {}", notificationId, e.getMessage());

            return NotificationResponse.builder()
                    .notificationId(notificationId)
                    .success(false)
                    .message("Failed to send email: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /** {@inheritDoc} */
    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    /**
     * Resolves the email content from the notification request.
     * Uses direct content if available, otherwise processes template with provided data.
     * 
     * @param request the notification request containing content or template information
     * @return the resolved email content
     */
    private String resolveContent(NotificationRequest request) {
        if (request.getContent() != null) {
            return request.getContent();
        }
        if (request.getTemplateName() != null && request.getTemplateData() != null) {
            return templateProcessor.process(request.getTemplateName(), request.getTemplateData());
        }
        return "";
    }
}