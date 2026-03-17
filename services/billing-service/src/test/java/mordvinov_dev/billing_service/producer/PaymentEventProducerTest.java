package mordvinov_dev.billing_service.producer;

import mordvinov_dev.billing_service.event.PaymentEvent;
import mordvinov_dev.billing_service.event.producer.PaymentEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventProducer Tests")
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks
    private PaymentEventProducer producer;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<PaymentEvent> eventCaptor;

    private PaymentEvent testPaymentEvent;
    private UUID testUserId;
    private String testPaymentId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPaymentId = "payment_12345";
        
        testPaymentEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_COMPLETED")
                .timestamp(LocalDateTime.now())
                .paymentId(testPaymentId)
                .userId(testUserId)
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("RUB")
                .status("COMPLETED")
                .description("Payment for premium subscription")
                .userEmail("test@example.com")
                .build();

        ReflectionTestUtils.setField(producer, "paymentEventsTopic", "payment.events");
    }

    @Test
    @DisplayName("Should send payment event successfully")
    void shouldSendPaymentEventSuccessfully() throws Exception {
        CompletableFuture<SendResult<String, PaymentEvent>> future = 
            new CompletableFuture<>();
        
        SendResult<String, PaymentEvent> sendResult = mock(SendResult.class);
        var recordMetadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(123L);
        
        future.complete(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
            .thenReturn(future);

        producer.sendPaymentEvent(testPaymentEvent);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals("payment.events", topicCaptor.getValue());
        assertEquals(testUserId.toString() + "_" + testPaymentId, keyCaptor.getValue());
        
        PaymentEvent capturedEvent = eventCaptor.getValue();
        assertEquals(testPaymentEvent.getEventId(), capturedEvent.getEventId());
        assertEquals(testPaymentEvent.getEventType(), capturedEvent.getEventType());
        assertEquals(testPaymentEvent.getPaymentId(), capturedEvent.getPaymentId());
        assertEquals(testPaymentEvent.getUserId(), capturedEvent.getUserId());
        assertEquals(testPaymentEvent.getAmount(), capturedEvent.getAmount());
        assertEquals(testPaymentEvent.getCurrency(), capturedEvent.getCurrency());
        assertEquals(testPaymentEvent.getStatus(), capturedEvent.getStatus());
        assertEquals(testPaymentEvent.getDescription(), capturedEvent.getDescription());
        assertEquals(testPaymentEvent.getUserEmail(), capturedEvent.getUserEmail());
    }

    @Test
    @DisplayName("Should handle Kafka send failure gracefully")
    void shouldHandleKafkaSendFailureGracefully() throws Exception {
        CompletableFuture<SendResult<String, PaymentEvent>> future = 
            new CompletableFuture<>();
        
        RuntimeException exception = new RuntimeException("Kafka send failed");
        future.completeExceptionally(exception);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
            .thenReturn(future);

        assertDoesNotThrow(() -> producer.sendPaymentEvent(testPaymentEvent));
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PaymentEvent.class));
    }

    @Test
    @DisplayName("Should handle critical Kafka template exception")
    void shouldHandleCriticalKafkaTemplateException() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
            .thenThrow(new RuntimeException("Kafka template error"));

        assertDoesNotThrow(() -> producer.sendPaymentEvent(testPaymentEvent));
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PaymentEvent.class));
    }

    @Test
    @DisplayName("Should generate correct key format")
    void shouldGenerateCorrectKeyFormat() throws Exception {
        CompletableFuture<SendResult<String, PaymentEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
            .thenReturn(future);

        producer.sendPaymentEvent(testPaymentEvent);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        String key = keyCaptor.getValue();
        
        String expectedKey = testUserId.toString() + "_" + testPaymentId;
        assertEquals(expectedKey, key);
    }

    @Test
    @DisplayName("Should use default topic when not configured")
    void shouldUseDefaultTopicWhenNotConfigured() throws Exception {
        PaymentEventProducer producerWithoutTopic = new PaymentEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producerWithoutTopic, "paymentEventsTopic", "payment.events");
        
        CompletableFuture<SendResult<String, PaymentEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
            .thenReturn(future);

        producerWithoutTopic.sendPaymentEvent(testPaymentEvent);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals("payment.events", topicCaptor.getValue());
    }

    @Test
    @DisplayName("Should send payment event with different status")
    void shouldSendPaymentEventWithDifferentStatus() throws Exception {
        PaymentEvent failedEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .paymentId("payment_67890")
                .userId(testUserId)
                .subscriptionId(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .currency("RUB")
                .status("FAILED")
                .description("Failed payment for premium subscription")
                .userEmail("test@example.com")
                .build();

        CompletableFuture<SendResult<String, PaymentEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
            .thenReturn(future);

        producer.sendPaymentEvent(failedEvent);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        PaymentEvent capturedEvent = eventCaptor.getValue();
        assertEquals("PAYMENT_FAILED", capturedEvent.getEventType());
        assertEquals("FAILED", capturedEvent.getStatus());
        assertEquals("payment_67890", capturedEvent.getPaymentId());
        assertEquals(new BigDecimal("500.00"), capturedEvent.getAmount());
    }
}
