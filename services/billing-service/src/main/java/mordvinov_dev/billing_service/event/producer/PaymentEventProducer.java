package mordvinov_dev.billing_service.event.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.event.PaymentEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for payment events.
 * Publishes payment status changes and other payment-related events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> paymentEventKafkaTemplate;

    @Value("${kafka.topics.payment-events:payment.events}")
    private String paymentEventsTopic;

    /**
     * Sends a payment event to Kafka.
     * @param event the payment event to send
     */
    public void sendPaymentEvent(PaymentEvent event) {
        log.info("Sending payment event: eventId={}, paymentId={}, status={}, userId={}",
                event.getEventId(), event.getPaymentId(), event.getStatus(), event.getUserId());

        String key = event.getUserId().toString() + "_" + event.getPaymentId();

        try {
            CompletableFuture<SendResult<String, PaymentEvent>> future =
                    paymentEventKafkaTemplate.send(paymentEventsTopic, key, event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send payment event, paymentId={}, status={}, error={}",
                            event.getPaymentId(), event.getStatus(), exception.getMessage(), exception);
                } else {
                    log.info("Payment event sent successfully, paymentId={}, status={}, partition={}, offset={}",
                            event.getPaymentId(), event.getStatus(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Critical error sending payment event, paymentId={}, status={}",
                    event.getPaymentId(), event.getStatus(), e);
        }
    }
}