package mordvinov_dev.worker_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String paymentId;
    private UUID userId;
    private UUID subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private String userEmail;
}