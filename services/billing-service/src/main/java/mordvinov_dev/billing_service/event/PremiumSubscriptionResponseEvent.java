package mordvinov_dev.billing_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumSubscriptionResponseEvent {
    private UUID eventId;
    private String eventType = "PREMIUM_SUBSCRIPTION_RESPONSE";
    private LocalDateTime timestamp;

    private UUID subscriptionId;
    private UUID userId;
    private String paymentId;
    private String status;
    private String message;
}