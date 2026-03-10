package mordvinov_dev.worker_service.service.audit.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.document.AuditLog;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.repository.AuditLogRepository;
import mordvinov_dev.worker_service.service.audit.AuditLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLog createAuditLog(PaymentEvent event) {
        log.debug("Creating audit log for event: {}", event.getEventId());

        AuditLog auditLog = AuditLog.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .paymentId(event.getPaymentId())
                .subscriptionId(event.getSubscriptionId())
                .action("PAYMENT_" + event.getStatus().toUpperCase())
                .status(event.getStatus())
                .details(String.format("Payment processed: amount=%.2f %s, description=%s",
                        event.getAmount(), event.getCurrency(),
                        event.getDescription() != null ? event.getDescription() : "N/A"))
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created: id={}, eventId={}", saved.getId(), event.getEventId());

        return saved;
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return auditLogRepository.existsByEventId(UUID.fromString(eventId));
    }
}