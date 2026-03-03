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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание возврата")
public class CreateRefundRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Schema(description = "Сумма возврата", example = "500.00", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Schema(description = "Валюта", example = "RUB", required = true)
    private String currency;

    @Size(max = 256, message = "Description cannot exceed 256 characters")
    @Schema(description = "Описание возврата", example = "Частичный возврат за неиспользованный период")
    private String description;

    @Schema(description = "Метаданные возврата")
    private Map<String, String> metadata;
}