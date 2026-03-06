package mordvinov_dev.billing_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.event.PremiumSubscriptionResponseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionProducer {

    private final KafkaTemplate<String, PremiumSubscriptionResponseEvent> premiumSubscriptionResponseKafkaTemplate;
    private static final String RESPONSE_TOPIC = "premium-subscription-response";

    public void sendSuccessResponse(UUID subscriptionId, UUID userId, PaymentResponse paymentResponse) {
        log.info("Sending success response, subscriptionId={}, userId={}, paymentId={}",
                subscriptionId, userId, paymentResponse.getPaymentId());

        PremiumSubscriptionResponseEvent event = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId(paymentResponse.getPaymentId())
                .confirmationUrl(paymentResponse.getConfirmationUrl())
                .status("PENDING")
                .message("Payment created successfully")
                .build();

        sendResponse(event, userId.toString() + "_" + event.getEventId());
    }

    public void sendFailureResponse(UUID subscriptionId, UUID userId, String errorMessage) {
        log.info("Sending failure response, subscriptionId={}, userId={}, error={}",
                subscriptionId, userId, errorMessage);

        PremiumSubscriptionResponseEvent event = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId(null)
                .confirmationUrl(null)
                .status("FAILED")
                .message(errorMessage)
                .build();

        sendResponse(event, userId.toString() + "_" + event.getEventId());
    }

    public void sendPaymentSuccessResponse(UUID subscriptionId, UUID userId, String paymentId) {
        log.info("Sending payment success response, subscriptionId={}, userId={}, paymentId={}",
                subscriptionId, userId, paymentId);

        PremiumSubscriptionResponseEvent event = PremiumSubscriptionResponseEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PREMIUM_SUBSCRIPTION_RESPONSE")
                .timestamp(LocalDateTime.now())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .paymentId(paymentId)
                .confirmationUrl(null)
                .status("SUCCESS")
                .message("Payment completed successfully")
                .build();

        sendResponse(event, userId.toString() + "_" + event.getEventId());
    }

    private void sendResponse(PremiumSubscriptionResponseEvent event, String key) {
        try {
            CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future =
                    premiumSubscriptionResponseKafkaTemplate.send(RESPONSE_TOPIC, key, event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send response, subscriptionId={}, userId={}, error={}",
                            event.getSubscriptionId(), event.getUserId(), exception.getMessage());
                } else {
                    log.info("Response sent successfully, subscriptionId={}, userId={}, status={}, partition={}, offset={}",
                            event.getSubscriptionId(), event.getUserId(), event.getStatus(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Critical error sending response, subscriptionId={}, userId={}",
                    event.getSubscriptionId(), event.getUserId(), e);
            throw new RuntimeException("Failed to send premium subscription response", e);
        }
    }
}