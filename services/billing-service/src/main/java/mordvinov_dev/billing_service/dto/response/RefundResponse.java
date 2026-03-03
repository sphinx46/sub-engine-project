package mordvinov_dev.billing_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с данными возврата")
public class RefundResponse {

    @Schema(description = "Внутренний ID записи возврата", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "ID возврата в ЮKassa", example = "216749f7-0016-50be-b000-078d43a63ae4")
    private String refundId;

    @Schema(description = "ID платежа в ЮKassa", example = "22e12f66-000f-5000-8000-18db351245c7")
    private String paymentId;

    @Schema(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Статус возврата", example = "succeeded")
    private String status;

    @Schema(description = "Сумма возврата", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Валюта", example = "RUB")
    private String currency;

    @Schema(description = "Описание возврата", example = "Частичный возврат за неиспользованный период")
    private String description;

    @Schema(description = "Дата создания", example = "2026-03-02T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "Дата последнего обновления", example = "2026-03-02T10:30:00Z")
    private Instant updatedAt;
}