package mordvinov_dev.worker_service.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@Schema(description = "Результат отправки уведомления")
public class NotificationResult {

    @Schema(description = "ID уведомления", example = "123e4567-e89b-12d3-a456-426614174000")
    String notificationId;

    @Schema(description = "Успешность отправки", example = "true")
    boolean success;

    @Schema(description = "Сообщение о результате", example = "Email sent successfully")
    String message;

    @Schema(description = "Время отправки")
    LocalDateTime timestamp;
}