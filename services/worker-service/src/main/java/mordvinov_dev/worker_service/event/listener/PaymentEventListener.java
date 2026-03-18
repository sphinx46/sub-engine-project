package mordvinov_dev.worker_service.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.event.PaymentEvent;
import mordvinov_dev.worker_service.service.notification.PaymentEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentEventProcessor eventProcessor;

    /**
     * Listens for payment events from Kafka and processes them.
     * Consumes messages from the payment events topic and delegates processing to the event processor.
     * 
     * @param event the payment event received from Kafka
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-events:payment.events}",
            groupId = "${spring.kafka.consumer.group-id:worker-service-group}",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    public void onPaymentEvent(@Payload PaymentEvent event) {
        log.info("Received: eventId={}, status={}", event.getEventId(), event.getStatus());
        eventProcessor.process(event);
    }
}