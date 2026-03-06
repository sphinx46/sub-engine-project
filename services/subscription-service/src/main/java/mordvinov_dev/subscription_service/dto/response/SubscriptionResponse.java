package mordvinov_dev.subscription_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с данными подписки")
public class SubscriptionResponse {

    @Schema(description = "ID подписки", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Тип плана", example = "PREMIUM")
    private PlanType planType;

    @Schema(description = "Статус подписки", example = "PENDING")
    private StatusType status;

    @Schema(description = "Дата следующего списания", example = "2026-04-02T10:30:00")
    private LocalDateTime nextBillingDate;

    @Schema(description = "Сообщение для пользователя", example = "Subscription created. Please complete payment to activate.")
    private String message;

    @Schema(description = "Дата создания", example = "2026-03-02T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления", example = "2026-03-02T10:30:00")
    private LocalDateTime updatedAt;
}