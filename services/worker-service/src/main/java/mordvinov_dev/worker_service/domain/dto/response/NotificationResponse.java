package mordvinov_dev.worker_service.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат отправки уведомления")
public class NotificationResponse {

    @Schema(description = "ID уведомления", example = "123e4567-e89b-12d3-a456-426614174000")
    private String notificationId;

    @Schema(description = "Успешность отправки", example = "true")
    private boolean success;

    @Schema(description = "Сообщение о результате", example = "Email sent successfully")
    private String message;

    @Schema(description = "Время отправки")
    private LocalDateTime timestamp;
}