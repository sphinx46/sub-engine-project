package mordvinov_dev.billing_service.service;

import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.dto.response.pageable.PageResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request, UUID userId);

    PaymentResponse getPayment(String paymentId, UUID userId);

    PaymentResponse getPaymentByYooKassaId(String paymentId, UUID userId);

    PageResponse<PaymentResponse> getPaymentsByUser(UUID userId, PageRequest pageRequest);

    PaymentResponse capturePayment(String paymentId, UUID userId);

    PaymentResponse cancelPayment(String paymentId, UUID userId);

    RefundResponse createRefund(CreateRefundRequest request, UUID userId);

    RefundResponse getRefund(String refundId, UUID userId);

    PageResponse<RefundResponse> getRefundsByPayment(String paymentId, UUID userId, PageRequest pageRequest);

    PageResponse<RefundResponse> getRefundsByUser(UUID userId, PageRequest pageRequest);
}