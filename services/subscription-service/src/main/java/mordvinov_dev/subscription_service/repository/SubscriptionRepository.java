package mordvinov_dev.subscription_service.repository;

import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.entity.enums.StatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findAllByUserId(UUID userId, Pageable pageable);

    Page<Subscription> findAllByUserIdAndStatus(UUID userId, StatusType status, Pageable pageable);

    List<Subscription> findAllByStatusAndNextBillingDateBefore(StatusType status, LocalDateTime date);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatus(UUID userId, StatusType status);
}