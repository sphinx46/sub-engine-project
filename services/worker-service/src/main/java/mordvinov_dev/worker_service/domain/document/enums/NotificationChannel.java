package mordvinov_dev.worker_service.domain.document.enums;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тип уведомления")
public enum NotificationChannel {
    @Schema(description = "Email уведомление")
    EMAIL,

    @Schema(description = "Push уведомление")
    PUSH
}