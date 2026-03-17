package mordvinov_dev.billing_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.dto.response.pageable.PageResponse;
import mordvinov_dev.billing_service.service.PaymentService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for payment and refund operations.
 * Provides endpoints for creating, managing, and retrieving payment information.
 */
@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment.
     * @param request payment creation request
     * @param userId user identifier from header
     * @return created payment response
     */
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Creating payment for user: {}, amount: {}", userId, request.getAmount());
        PaymentResponse response = paymentService.createPayment(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a payment by its identifier.
     * @param paymentId payment identifier
     * @param userId user identifier from header
     * @return payment response
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String paymentId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("Fetching payment: {} for user: {}", paymentId, userId);
        PaymentResponse response = paymentService.getPayment(paymentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of user payments.
     * @param userId user identifier from header
     * @param size page size
     * @param pageNumber page number
     * @param sortBy sort field
     * @param direction sort direction
     * @return paginated payment list
     */
    @GetMapping("/payments")
    public ResponseEntity<PageResponse<PaymentResponse>> getUserPayments(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "20", required = false) @Min(1) Integer size,
            @RequestParam(defaultValue = "0", required = false) @Min(0) Integer pageNumber,
            @RequestParam(defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(defaultValue = "DESC", required = false) String direction) {

        log.debug("Fetching payments for user: {}, page: {}", userId, pageNumber);

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .pageNumber(pageNumber)
                .sortBy(sortBy)
                .direction(Sort.Direction.fromString(direction))
                .build();

        PageResponse<PaymentResponse> response = paymentService.getPaymentsByUser(userId, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Captures (confirms) a payment.
     * @param paymentId payment identifier
     * @param userId user identifier from header
     * @return updated payment response
     */
    @PostMapping("/payments/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @PathVariable String paymentId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Capturing payment: {} for user: {}", paymentId, userId);
        PaymentResponse response = paymentService.capturePayment(paymentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a payment.
     * @param paymentId payment identifier
     * @param userId user identifier from header
     * @return updated payment response
     */
    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable String paymentId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Canceling payment: {} for user: {}", paymentId, userId);
        PaymentResponse response = paymentService.cancelPayment(paymentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a payment from YooKassa by its identifier.
     * @param paymentId YooKassa payment identifier
     * @param userId user identifier from header
     * @return payment response
     */
    @GetMapping("/payments/yookassa/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentFromYooKassa(
            @PathVariable String paymentId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("Fetching payment from YooKassa: {} for user: {}", paymentId, userId);
        PaymentResponse response = paymentService.getPaymentByYooKassaId(paymentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new refund.
     * @param request refund creation request
     * @param userId user identifier from header
     * @return created refund response
     */
    @PostMapping("/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Creating refund for payment: {}, amount: {}", request.getPaymentId(), request.getAmount());
        RefundResponse response = paymentService.createRefund(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a refund by its identifier.
     * @param refundId refund identifier
     * @param userId user identifier from header
     * @return refund response
     */
    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(
            @PathVariable String refundId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("Fetching refund: {} for user: {}", refundId, userId);
        RefundResponse response = paymentService.getRefund(refundId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of refunds for a specific payment.
     * @param paymentId payment identifier
     * @param userId user identifier from header
     * @param size page size
     * @param pageNumber page number
     * @param sortBy sort field
     * @param direction sort direction
     * @return paginated refund list
     */
    @GetMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<PageResponse<RefundResponse>> getRefundsByPayment(
            @PathVariable String paymentId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "20", required = false) @Min(1) Integer size,
            @RequestParam(defaultValue = "0", required = false) @Min(0) Integer pageNumber,
            @RequestParam(defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(defaultValue = "DESC", required = false) String direction) {

        log.debug("Fetching refunds for payment: {}, user: {}, page: {}", paymentId, userId, pageNumber);

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .pageNumber(pageNumber)
                .sortBy(sortBy)
                .direction(Sort.Direction.fromString(direction))
                .build();

        PageResponse<RefundResponse> response = paymentService.getRefundsByPayment(paymentId, userId, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of user refunds.
     * @param userId user identifier from header
     * @param size page size
     * @param pageNumber page number
     * @param sortBy sort field
     * @param direction sort direction
     * @return paginated refund list
     */
    @GetMapping("/refunds")
    public ResponseEntity<PageResponse<RefundResponse>> getUserRefunds(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "20", required = false) @Min(1) Integer size,
            @RequestParam(defaultValue = "0", required = false) @Min(0) Integer pageNumber,
            @RequestParam(defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(defaultValue = "DESC", required = false) String direction) {

        log.debug("Fetching refunds for user: {}, page: {}", userId, pageNumber);

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .pageNumber(pageNumber)
                .sortBy(sortBy)
                .direction(Sort.Direction.fromString(direction))
                .build();

        PageResponse<RefundResponse> response = paymentService.getRefundsByUser(userId, pageRequest);
        return ResponseEntity.ok(response);
    }
}