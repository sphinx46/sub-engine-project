package mordvinov_dev.billing_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.event.PaymentEvent;
import mordvinov_dev.billing_service.exception.PaymentNotFoundException;
import mordvinov_dev.billing_service.producer.PaymentEventProducer;
import mordvinov_dev.billing_service.producer.PremiumSubscriptionProducer;
import mordvinov_dev.billing_service.repository.PaymentRepository;
import mordvinov_dev.billing_service.service.WebhookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PremiumSubscriptionProducer premiumSubscriptionProducer;

    @Override
    @Transactional
    public void processWebhook(JsonNode payload) {
        String eventType = payload.has("event") ? payload.get("event").asText() : "unknown";
        String paymentId = payload.has("object") ? payload.get("object").get("id").asText() : null;

        log.info("Processing webhook event: {}, paymentId: {}", eventType, paymentId);

        if (paymentId == null) {
            log.error("Payment ID not found in webhook payload");
            return;
        }

        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        String oldStatus = payment.getStatus();
        String newStatus = mapYooKassaStatus(eventType);

        if (newStatus != null && !newStatus.equals(oldStatus)) {
            payment.setStatus(newStatus);
            payment.setUpdatedAt(LocalDateTime.now());

            if ("succeeded".equalsIgnoreCase(newStatus) && payment.getSubscriptionId() != null) {
                log.info("Payment succeeded for subscription: {}, sending premium subscription response", payment.getSubscriptionId());
                premiumSubscriptionProducer.sendPaymentSuccessResponse(
                        payment.getSubscriptionId(),
                        payment.getUserId(),
                        payment.getPaymentId()
                );
            }

            log.info("Payment status updated from {} to {} for paymentId: {}", oldStatus, newStatus, paymentId);
        }

        String userEmail = extractUserEmail(payload, payment);

        PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_" + eventType.toUpperCase().replace(".", "_"))
                .timestamp(LocalDateTime.now())
                .paymentId(payment.getPaymentId())
                .userId(payment.getUserId())
                .subscriptionId(payment.getSubscriptionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .userEmail(userEmail)
                .build();

        log.info("Sending payment event to Kafka for paymentId: {}, status: {}, userEmail: {}",
                paymentId, event.getStatus(), userEmail);
        paymentEventProducer.sendPaymentEvent(event);
    }

    private String extractUserEmail(JsonNode payload, PaymentEntity payment) {
        if (payment.getUserEmail() != null && !payment.getUserEmail().isEmpty()) {
            return payment.getUserEmail();
        }

        try {
            if (payload.has("object") && payload.get("object").has("metadata")) {
                JsonNode metadata = payload.get("object").get("metadata");
                if (metadata.has("userEmail")) {
                    return metadata.get("userEmail").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract email from metadata: {}", e.getMessage());
        }

        return null;
    }

    private String mapYooKassaStatus(String yooKassaEvent) {
        return switch (yooKassaEvent) {
            case "payment.succeeded" -> "succeeded";
            case "payment.canceled" -> "canceled";
            case "payment.waiting_for_capture" -> "waiting_for_capture";
            default -> null;
        };
    }
}