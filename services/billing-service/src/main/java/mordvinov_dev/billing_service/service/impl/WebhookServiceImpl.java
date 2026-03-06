package mordvinov_dev.billing_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.entity.RefundEntity;
import mordvinov_dev.billing_service.producer.PremiumSubscriptionProducer;
import mordvinov_dev.billing_service.repository.PaymentRepository;
import mordvinov_dev.billing_service.repository.RefundRepository;
import mordvinov_dev.billing_service.service.WebhookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PremiumSubscriptionProducer premiumSubscriptionProducer;

    @Override
    @Transactional
    public void processWebhook(JsonNode payload) {
        String event = payload.has("event") ? payload.get("event").asText() : null;

        if (event == null) {
            log.warn("Received webhook without event type");
            return;
        }

        log.info("Processing webhook event: {}", event);

        if (event.startsWith("payment.")) {
            processPaymentWebhook(payload, event);
        } else if (event.startsWith("refund.")) {
            processRefundWebhook(payload, event);
        } else {
            log.warn("Unhandled webhook event type: {}", event);
        }
    }

    private void processPaymentWebhook(JsonNode payload, String event) {
        JsonNode object = payload.get("object");
        if (object == null || !object.has("id")) {
            log.warn("Payment webhook missing object or id");
            return;
        }

        String paymentId = object.get("id").asText();
        String newStatus = object.has("status") ? object.get("status").asText() : null;

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPaymentId(paymentId);

        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for webhook: {}", paymentId);
            return;
        }

        PaymentEntity payment = paymentOpt.get();
        String oldStatus = payment.getStatus();

        if (newStatus != null && !newStatus.equals(oldStatus)) {
            payment.setStatus(newStatus);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Updated payment {} status from {} to {}", paymentId, oldStatus, newStatus);

            if ("succeeded".equals(newStatus) && payment.getSubscriptionId() != null) {
                premiumSubscriptionProducer.sendPaymentSuccessResponse(
                        payment.getSubscriptionId(),
                        payment.getUserId(),
                        paymentId
                );
                log.info("Payment success notification sent for subscription {}", payment.getSubscriptionId());
            }
        }
    }

    private void processRefundWebhook(JsonNode payload, String event) {
        JsonNode object = payload.get("object");
        if (object == null || !object.has("id")) {
            log.warn("Refund webhook missing object or id");
            return;
        }

        String refundId = object.get("id").asText();
        String newStatus = object.has("status") ? object.get("status").asText() : null;

        Optional<RefundEntity> refundOpt = refundRepository.findByRefundId(refundId);

        if (refundOpt.isEmpty()) {
            log.warn("Refund not found for webhook: {}", refundId);
            return;
        }

        RefundEntity refund = refundOpt.get();
        String oldStatus = refund.getStatus();

        if (newStatus != null && !newStatus.equals(oldStatus)) {
            refund.setStatus(newStatus);
            refund.setUpdatedAt(LocalDateTime.now());
            refundRepository.save(refund);

            log.info("Updated refund {} status from {} to {}", refundId, oldStatus, newStatus);
        }
    }
}