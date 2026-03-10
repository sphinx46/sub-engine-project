package mordvinov_dev.worker_service.notification.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import mordvinov_dev.worker_service.notification.domain.NotificationType;

import java.util.Map;
import java.util.UUID;

@Value
@Builder
@Schema(description = "Запрос на отправку уведомления")
public class NotificationRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    UUID userId;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Тип уведомления", required = true)
    NotificationType type;

    @NotBlank(message = "Recipient is required")
    @Schema(description = "Получатель (email/токен)", example = "user@example.com", required = true)
    String recipient;

    @NotBlank(message = "Subject is required")
    @Schema(description = "Тема уведомления", example = "Статус платежа", required = true)
    String subject;

    @Schema(description = "Имя шаблона", example = "payment-success")
    String templateName;

    @Schema(description = "Данные для шаблона")
    Map<String, Object> templateData;

    @Schema(description = "Текст уведомления (если не используется шаблон)", example = "Ваш платеж успешно выполнен")
    String content;
}