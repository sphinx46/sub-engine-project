package mordvinov_dev.billing_service.event;

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
public class PremiumSubscriptionRequestEvent {
    private UUID eventId;
    private String eventType = "PREMIUM_SUBSCRIPTION_REQUEST";
    private LocalDateTime timestamp;
    private UUID subscriptionId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String userEmail;
}