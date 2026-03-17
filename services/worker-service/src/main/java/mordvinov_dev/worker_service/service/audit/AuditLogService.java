package mordvinov_dev.worker_service.service.audit;

import mordvinov_dev.worker_service.domain.document.AuditLog;
import mordvinov_dev.worker_service.event.PaymentEvent;

/**
 * Service interface for managing audit logs related to payment events.
 * Provides methods for creating audit records and checking their existence.
 */
public interface AuditLogService {
    /**
     * Creates an audit log entry for a payment event.
     * 
     * @param event the payment event to create an audit log for
     * @return the created audit log entry
     */
    AuditLog createAuditLog(PaymentEvent event);
    
    /**
     * Checks if an audit log exists for the specified event ID.
     * 
     * @param eventId the event ID to search for
     * @return true if an audit log exists, false otherwise
     */
    boolean existsByEventId(String eventId);
}