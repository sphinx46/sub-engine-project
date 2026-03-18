package mordvinov_dev.billing_service.repository;

import mordvinov_dev.billing_service.entity.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByPaymentIdAndUserId(String paymentId, UUID userId);
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    Page<PaymentEntity> findAllByUserId(UUID userId, Pageable pageable);
    boolean existsByPaymentId(String paymentId);
}