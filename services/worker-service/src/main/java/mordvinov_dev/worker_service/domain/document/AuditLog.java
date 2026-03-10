package mordvinov_dev.worker_service.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    @Indexed
    private UUID eventId;
    @Indexed
    private String eventType;
    @Indexed
    private UUID userId;
    private String paymentId;
    private UUID subscriptionId;
    private String action;
    private String status;
    private String details;
    private LocalDateTime timestamp;
}