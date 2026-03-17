package mordvinov_dev.billing_service.service;

import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.dto.response.pageable.PageResponse;

import java.util.UUID;

/**
 * Service for managing payments and refunds.
 * Provides functionality for creating, processing and tracking payment operations.
 */
public interface PaymentService {
    /**
     * Creates a new payment.
     * @param request payment creation data
     * @param userId user identifier
     * @return created payment
     */
    PaymentResponse createPayment(CreatePaymentRequest request, UUID userId);

    /**
     * Retrieves a payment by its identifier.
     * @param paymentId payment identifier
     * @param userId user identifier
     * @return payment information
     */
    PaymentResponse getPayment(String paymentId, UUID userId);

    /**
     * Retrieves a payment by YooKassa identifier.
     * @param paymentId YooKassa payment identifier
     * @param userId user identifier
     * @return payment information
     */
    PaymentResponse getPaymentByYooKassaId(String paymentId, UUID userId);

    /**
     * Retrieves a paginated list of user payments.
     * @param userId user identifier
     * @param pageRequest pagination parameters
     * @return page with user payments
     */
    PageResponse<PaymentResponse> getPaymentsByUser(UUID userId, PageRequest pageRequest);

    /**
     * Confirms (captures) a payment.
     * @param paymentId payment identifier
     * @param userId user identifier
     * @return updated payment information
     */
    PaymentResponse capturePayment(String paymentId, UUID userId);

    /**
     * Cancels a payment.
     * @param paymentId payment identifier
     * @param userId user identifier
     * @return updated payment information
     */
    PaymentResponse cancelPayment(String paymentId, UUID userId);

    /**
     * Creates a refund.
     * @param request refund creation data
     * @param userId user identifier
     * @return created refund
     */
    RefundResponse createRefund(CreateRefundRequest request, UUID userId);

    /**
     * Retrieves refund information.
     * @param refundId refund identifier
     * @param userId user identifier
     * @return refund information
     */
    RefundResponse getRefund(String refundId, UUID userId);

    /**
     * Retrieves a paginated list of refunds by payment.
     * @param paymentId payment identifier
     * @param userId user identifier
     * @param pageRequest pagination parameters
     * @return page with refunds by payment
     */
    PageResponse<RefundResponse> getRefundsByPayment(String paymentId, UUID userId, PageRequest pageRequest);

    /**
     * Retrieves a paginated list of user refunds.
     * @param userId user identifier
     * @param pageRequest pagination parameters
     * @return page with user refunds
     */
    PageResponse<RefundResponse> getRefundsByUser(UUID userId, PageRequest pageRequest);
}