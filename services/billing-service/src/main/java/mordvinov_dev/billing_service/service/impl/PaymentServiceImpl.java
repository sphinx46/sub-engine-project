package mordvinov_dev.billing_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.config.YooKassaConfig;
import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.dto.response.pageable.PageResponse;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.entity.RefundEntity;
import mordvinov_dev.billing_service.exception.PaymentNotFoundException;
import mordvinov_dev.billing_service.exception.RefundNotFoundException;
import mordvinov_dev.billing_service.exception.UnauthorizedPaymentAccessException;
import mordvinov_dev.billing_service.exception.YooKassaException;
import mordvinov_dev.billing_service.mapping.EntityMapper;
import mordvinov_dev.billing_service.repository.PaymentRepository;
import mordvinov_dev.billing_service.repository.RefundRepository;
import mordvinov_dev.billing_service.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.model.Refund;
import ru.loolzaaa.youkassa.pojo.Amount;
import ru.loolzaaa.youkassa.pojo.Confirmation;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;
import ru.loolzaaa.youkassa.processors.RefundProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final ApiClient apiClient;
    private final YooKassaConfig config;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, UUID userId) {
        try {
            PaymentProcessor paymentProcessor = new PaymentProcessor(apiClient);

            Amount amount = Amount.builder()
                    .value(request.getAmount().toString())
                    .currency(request.getCurrency())
                    .build();

            Confirmation confirmation = Confirmation.builder()
                    .type(Confirmation.Type.REDIRECT)
                    .returnUrl(config.getReturnUrl())
                    .build();

            Payment payment = Payment.builder()
                    .amount(amount)
                    .description(request.getDescription())
                    .confirmation(confirmation)
                    .savePaymentMethod(request.getSavePaymentMethod() != null ? request.getSavePaymentMethod() : false)
                    .capture(request.getCapture() != null ? request.getCapture() : true)
                    .metadata(request.getMetadata() != null ? request.getMetadata() : null)
                    .build();

            Payment createdPayment = paymentProcessor.create(payment, null);

            PaymentEntity entity = PaymentEntity.builder()
                    .paymentId(createdPayment.getId())
                    .userId(userId)
                    .subscriptionId(request.getSubscriptionId())
                    .status(createdPayment.getStatus())
                    .amount(new BigDecimal(createdPayment.getAmount().getValue()))
                    .currency(createdPayment.getAmount().getCurrency())
                    .description(createdPayment.getDescription())
                    .paymentMethodId(createdPayment.getPaymentMethod() != null ? createdPayment.getPaymentMethod().getId() : null)
                    .paymentMethodType(createdPayment.getPaymentMethod() != null ? createdPayment.getPaymentMethod().getType() : null)
                    .build();

            paymentRepository.save(entity);

            PaymentResponse response = entityMapper.map(entity, PaymentResponse.class);
            response.setConfirmationUrl(createdPayment.getConfirmation().getConfirmationUrl());

            log.info("Payment created successfully: {} for user: {}", createdPayment.getId(), userId);
            return response;

        } catch (Exception e) {
            log.error("Failed to create payment for user: {}", userId, e);
            throw new YooKassaException("Failed to create payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId, UUID userId) {
        PaymentEntity paymentEntity = paymentRepository.findByPaymentIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        return entityMapper.map(paymentEntity, PaymentResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentsByUser(UUID userId, PageRequest pageRequest) {
        Page<PaymentEntity> paymentsPage = paymentRepository.findAllByUserId(userId, pageRequest.toPageable());

        return PageResponse.<PaymentResponse>builder()
                .content(entityMapper.mapList(paymentsPage.getContent(), PaymentResponse.class))
                .currentPage(paymentsPage.getNumber())
                .totalPages(paymentsPage.getTotalPages())
                .totalElements(paymentsPage.getTotalElements())
                .pageSize(paymentsPage.getSize())
                .first(paymentsPage.isFirst())
                .last(paymentsPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse capturePayment(String paymentId, UUID userId) {
        PaymentEntity paymentEntity = paymentRepository.findByPaymentIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        try {
            PaymentProcessor paymentProcessor = new PaymentProcessor(apiClient);
            Payment currentPayment = paymentProcessor.findById(paymentId);

            if (!"waiting_for_capture".equals(currentPayment.getStatus())) {
                log.error("Payment with id {} status is not waiting_for_capture, current status: {}",
                        paymentId, currentPayment.getStatus());
                throw new YooKassaException("Payment status is not waiting_for_capture");
            }

            Payment capturedPayment = paymentProcessor.capture(paymentId, Payment.builder().build(), null);

            paymentEntity.setStatus(capturedPayment.getStatus());
            paymentEntity.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(paymentEntity);

            log.info("Payment captured successfully: {} for user: {}", paymentId, userId);
            return entityMapper.map(paymentEntity, PaymentResponse.class);

        } catch (YooKassaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to capture payment: {} for user: {}", paymentId, userId, e);
            throw new YooKassaException("Failed to capture payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(String paymentId, UUID userId) {
        PaymentEntity paymentEntity = paymentRepository.findByPaymentIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        try {
            PaymentProcessor paymentProcessor = new PaymentProcessor(apiClient);
            Payment currentPayment = paymentProcessor.findById(paymentId);

            if (!"waiting_for_capture".equals(currentPayment.getStatus()) &&
                    !"pending".equals(currentPayment.getStatus())) {
                log.error("Payment with id {} cannot be cancelled, current status: {}",
                        paymentId, currentPayment.getStatus());
                throw new YooKassaException("Payment cannot be cancelled in current status");
            }

            Payment cancelledPayment = paymentProcessor.cancel(paymentId, null);

            paymentEntity.setStatus(cancelledPayment.getStatus());
            paymentEntity.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(paymentEntity);

            log.info("Payment cancelled successfully: {} for user: {}", paymentId, userId);
            return entityMapper.map(paymentEntity, PaymentResponse.class);

        } catch (YooKassaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel payment: {} for user: {}", paymentId, userId, e);
            throw new YooKassaException("Failed to cancel payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RefundResponse createRefund(CreateRefundRequest request, UUID userId) {
        try {
            if (request.getPaymentId() == null) {
                throw new YooKassaException("Payment ID is required for refund");
            }

            PaymentEntity paymentEntity = paymentRepository.findByPaymentIdAndUserId(request.getPaymentId(), userId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + request.getPaymentId()));

            RefundProcessor refundProcessor = new RefundProcessor(apiClient);

            Amount amount = Amount.builder()
                    .value(request.getAmount().toString())
                    .currency(request.getCurrency())
                    .build();

            Refund refund = Refund.builder()
                    .paymentId(request.getPaymentId())
                    .amount(amount)
                    .description(request.getDescription())
                    .build();

            Refund createdRefund = refundProcessor.create(refund, null);

            RefundEntity refundEntity = RefundEntity.builder()
                    .refundId(createdRefund.getId())
                    .paymentId(createdRefund.getPaymentId())
                    .userId(userId)
                    .status(createdRefund.getStatus())
                    .amount(new BigDecimal(createdRefund.getAmount().getValue()))
                    .currency(createdRefund.getAmount().getCurrency())
                    .description(createdRefund.getDescription())
                    .refundMethodType(createdRefund.getRefundMethod() != null ?
                            createdRefund.getRefundMethod().getType() : null)
                    .build();

            refundRepository.save(refundEntity);

            log.info("Refund created successfully: {} for payment: {} for user: {}",
                    createdRefund.getId(), request.getPaymentId(), userId);

            return entityMapper.map(refundEntity, RefundResponse.class);

        } catch (Exception e) {
            log.error("Failed to create refund for payment: {} for user: {}",
                    request.getPaymentId() != null ? request.getPaymentId() : "unknown", userId, e);
            throw new YooKassaException("Failed to create refund: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefund(String refundId, UUID userId) {
        RefundEntity refundEntity = refundRepository.findByRefundIdAndUserId(refundId, userId)
                .orElseThrow(() -> new RefundNotFoundException("Refund not found with id: " + refundId));

        return entityMapper.map(refundEntity, RefundResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getRefundsByPayment(String paymentId, UUID userId, PageRequest pageRequest) {
        if (!paymentRepository.existsByPaymentId(paymentId)) {
            throw new PaymentNotFoundException("Payment not found with id: " + paymentId);
        }

        Page<RefundEntity> refundsPage = refundRepository.findAllByPaymentId(paymentId, pageRequest.toPageable());

        return PageResponse.<RefundResponse>builder()
                .content(entityMapper.mapList(refundsPage.getContent(), RefundResponse.class))
                .currentPage(refundsPage.getNumber())
                .totalPages(refundsPage.getTotalPages())
                .totalElements(refundsPage.getTotalElements())
                .pageSize(refundsPage.getSize())
                .first(refundsPage.isFirst())
                .last(refundsPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getRefundsByUser(UUID userId, PageRequest pageRequest) {
        Page<RefundEntity> refundsPage = refundRepository.findAllByUserId(userId, pageRequest.toPageable());

        return PageResponse.<RefundResponse>builder()
                .content(entityMapper.mapList(refundsPage.getContent(), RefundResponse.class))
                .currentPage(refundsPage.getNumber())
                .totalPages(refundsPage.getTotalPages())
                .totalElements(refundsPage.getTotalElements())
                .pageSize(refundsPage.getSize())
                .first(refundsPage.isFirst())
                .last(refundsPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByYooKassaId(String paymentId, UUID userId) {
        try {
            PaymentProcessor paymentProcessor = new PaymentProcessor(apiClient);
            Payment payment = paymentProcessor.findById(paymentId);

            PaymentEntity paymentEntity = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

            if (!paymentEntity.getUserId().equals(userId)) {
                throw new UnauthorizedPaymentAccessException("User " + userId + " does not have access to payment " + paymentId);
            }

            paymentEntity.setStatus(payment.getStatus());
            paymentEntity.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(paymentEntity);

            return entityMapper.map(paymentEntity, PaymentResponse.class);

        } catch (Exception e) {
            log.error("Failed to fetch payment from YooKassa: {}", paymentId, e);
            throw new YooKassaException("Failed to fetch payment from YooKassa: " + e.getMessage());
        }
    }
}