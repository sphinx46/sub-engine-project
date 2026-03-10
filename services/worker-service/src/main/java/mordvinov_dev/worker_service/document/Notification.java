package mordvinov_dev.worker_service.document;

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
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    @Indexed
    private UUID userId;
    @Indexed
    private String type;
    private String channel;
    private String recipient;
    private String subject;
    private String content;
    private Boolean sent;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}