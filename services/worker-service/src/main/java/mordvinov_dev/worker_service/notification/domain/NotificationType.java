package mordvinov_dev.worker_service.notification.domain;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тип уведомления")
public enum NotificationType {
    @Schema(description = "Email уведомление")
    EMAIL,

    @Schema(description = "Push уведомление")
    PUSH
}