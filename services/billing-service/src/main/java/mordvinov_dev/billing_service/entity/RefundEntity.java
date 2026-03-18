package mordvinov_dev.billing_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "refunds", indexes = {
        @Index(name = "idx_refunds_payment_id", columnList = "payment_id"),
        @Index(name = "idx_refunds_user_id", columnList = "user_id"),
        @Index(name = "idx_refunds_refund_id", columnList = "refund_id"),
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEntity extends BaseEntity {

    @Column(name = "refund_id", nullable = false, unique = true)
    private String refundId;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 256)
    private String description;

    @Column(name = "refund_method_type")
    private String refundMethodType;
}