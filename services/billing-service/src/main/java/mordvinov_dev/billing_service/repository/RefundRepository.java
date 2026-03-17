package mordvinov_dev.billing_service.repository;

import mordvinov_dev.billing_service.entity.RefundEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
    Optional<RefundEntity> findByRefundId(String refundId);
    Optional<RefundEntity> findByRefundIdAndUserId(String refundId, UUID userId);
    Page<RefundEntity> findAllByPaymentId(String paymentId, Pageable pageable);
    Page<RefundEntity> findAllByUserId(UUID userId, Pageable pageable);
}