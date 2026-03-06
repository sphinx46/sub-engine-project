package mordvinov_dev.billing_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.event.PremiumSubscriptionRequestEvent;
import mordvinov_dev.billing_service.producer.PremiumSubscriptionProducer;
import mordvinov_dev.billing_service.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionRequestListener {

    private final PaymentService paymentService;
    private final PremiumSubscriptionProducer premiumSubscriptionProducer;

    @KafkaListener(
            topics = "${kafka.topics.premium-subscription-request:premium-subscription-request}",
            groupId = "${spring.kafka.consumer.group-id:billing-service-group}",
            containerFactory = "premiumSubscriptionRequestKafkaListenerContainerFactory"
    )
    public void onPremiumSubscriptionRequest(@Payload PremiumSubscriptionRequestEvent event) {
        log.info("Received premium subscription request, eventId={}, userId={}, subscriptionId={}, amount={}",
                event.getEventId(), event.getUserId(), event.getSubscriptionId(), event.getAmount());

        try {
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("userId", event.getUserId().toString());
            metadata.put("subscriptionId", event.getSubscriptionId().toString());
            metadata.put("eventId", event.getEventId().toString());

            CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .description(event.getDescription())
                    .subscriptionId(event.getSubscriptionId())
                    .capture(true)
                    .savePaymentMethod(false)
                    .metadata(metadata)
                    .build();

            PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, event.getUserId());

            log.info("Premium subscription request processed successfully, paymentId={}, userId={}, confirmationUrl={}",
                    paymentResponse.getPaymentId(), event.getUserId(), paymentResponse.getConfirmationUrl());

        } catch (Exception e) {
            log.error("Error processing premium subscription request, userId={}, subscriptionId={}, error={}",
                    event.getUserId(), event.getSubscriptionId(), e.getMessage(), e);
        }
    }
}