package mordvinov_dev.worker_service.service.audit;

import mordvinov_dev.worker_service.domain.document.AuditLog;
import mordvinov_dev.worker_service.event.PaymentEvent;

public interface AuditLogService {
    AuditLog createAuditLog(PaymentEvent event);
    boolean existsByEventId(String eventId);
}