package mordvinov_dev.billing_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание платежа")
public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    @Schema(description = "Сумма платежа", example = "1000.00", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Schema(description = "Валюта", example = "RUB", required = true)
    private String currency;

    @NotBlank(message = "Description is required")
    @Size(max = 128, message = "Description cannot exceed 128 characters")
    @Schema(description = "Описание платежа", example = "Оплата подписки PREMIUM", required = true)
    private String description;

    @Schema(description = "ID подписки", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID subscriptionId;

    @Schema(description = "Флаг автоматического подтверждения", example = "true", defaultValue = "true")
    private Boolean capture;

    @Schema(description = "Сохранить способ оплаты для автоплатежей", example = "false")
    private Boolean savePaymentMethod;

    @Schema(description = "Метаданные платежа")
    private Map<String, String> metadata;
}