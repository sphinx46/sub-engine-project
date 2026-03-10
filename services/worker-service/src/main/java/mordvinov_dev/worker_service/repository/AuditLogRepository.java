package mordvinov_dev.worker_service.repository;

import mordvinov_dev.worker_service.domain.document.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    boolean existsByEventId(UUID eventId);
}