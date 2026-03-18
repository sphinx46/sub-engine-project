package mordvinov_dev.subscription_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mordvinov_dev.subscription_service.entity.enums.PlanType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание подписки")
public class CreateSubscriptionRequest {

    @NotNull(message = "Тип плана обязателен")
    @Schema(description = "Тип подписки",
            example = "BASIC",
            allowableValues = {"FREE", "PREMIUM"})
    private PlanType planType;
}