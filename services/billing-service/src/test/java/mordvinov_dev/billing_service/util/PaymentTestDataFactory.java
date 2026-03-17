package mordvinov_dev.billing_service.util;

import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.entity.RefundEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class PaymentTestDataFactory {

    public static final String TEST_PAYMENT_ID = "test-payment-id";
    public static final String TEST_REFUND_ID = "test-refund-id";
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String DEFAULT_CURRENCY = "RUB";
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("1000.00");

    public static CreatePaymentRequest createPaymentRequest() {
        return CreatePaymentRequest.builder()
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment")
                .subscriptionId(UUID.randomUUID())
                .capture(true)
                .savePaymentMethod(false)
                .metadata(Map.of("userEmail", TEST_USER_EMAIL))
                .build();
    }

    public static CreateRefundRequest createRefundRequest() {
        return CreateRefundRequest.builder()
                .paymentId(TEST_PAYMENT_ID)
                .amount(new BigDecimal("500.00"))
                .currency(DEFAULT_CURRENCY)
                .description("Test refund")
                .build();
    }

    public static CreateRefundRequest createRefundRequestWithNullPaymentId() {
        return CreateRefundRequest.builder()
                .paymentId(null)
                .amount(new BigDecimal("500.00"))
                .currency(DEFAULT_CURRENCY)
                .description("Test refund with null payment ID")
                .build();
    }

    public static CreateRefundRequest createRefundRequestWithZeroAmount() {
        return CreateRefundRequest.builder()
                .paymentId(TEST_PAYMENT_ID)
                .amount(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .description("Zero amount refund")
                .build();
    }

    public static CreateRefundRequest createRefundRequestWithNegativeAmount() {
        return CreateRefundRequest.builder()
                .paymentId(TEST_PAYMENT_ID)
                .amount(new BigDecimal("-100.00"))
                .currency(DEFAULT_CURRENCY)
                .description("Negative amount refund")
                .build();
    }

    public static PaymentEntity createPaymentEntity(UUID userId) {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(userId)
                .subscriptionId(UUID.randomUUID())
                .status("pending")
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static PaymentEntity createPaymentEntityWithDifferentUser(UUID differentUserId) {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(differentUserId)
                .subscriptionId(UUID.randomUUID())
                .status("pending")
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment")
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static PaymentEntity createPaymentEntityWithStatus(String status) {
        return PaymentEntity.builder()
                .paymentId(TEST_PAYMENT_ID)
                .userId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .status(status)
                .amount(DEFAULT_AMOUNT)
                .currency(DEFAULT_CURRENCY)
                .description("Test payment with status: " + status)
                .userEmail(TEST_USER_EMAIL)
                .build();
    }

    public static RefundEntity createRefundEntity(UUID userId) {
        return RefundEntity.builder()
                .refundId(TEST_REFUND_ID)
                .paymentId(TEST_PAYMENT_ID)
                .userId(userId)
                .status("succeeded")
                .amount(new BigDecimal("500.00"))
                .currency(DEFAULT_CURRENCY)
                .description("Test refund")
                .build();
    }

    public static PageRequest createPageRequest() {
        return PageRequest.builder()
                .pageNumber(0)
                .size(10)
                .sortBy("createdAt")
                .direction(org.springframework.data.domain.Sort.Direction.DESC)
                .build();
    }

    public static PageRequest createLargePageRequest() {
        return PageRequest.builder()
                .pageNumber(100)
                .size(1000)
                .sortBy("createdAt")
                .direction(org.springframework.data.domain.Sort.Direction.ASC)
                .build();
    }
}
