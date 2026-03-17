package mordvinov_dev.billing_service.producer;

import mordvinov_dev.billing_service.event.PremiumSubscriptionResponseEvent;
import mordvinov_dev.billing_service.event.producer.PremiumSubscriptionProducer;
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

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PremiumSubscriptionProducer Tests")
class PremiumSubscriptionProducerTest {

    @Mock
    private KafkaTemplate<String, PremiumSubscriptionResponseEvent> kafkaTemplate;

    @InjectMocks
    private PremiumSubscriptionProducer producer;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<PremiumSubscriptionResponseEvent> eventCaptor;

    private UUID testSubscriptionId;
    private UUID testUserId;
    private String testPaymentId;

    @BeforeEach
    void setUp() {
        testSubscriptionId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testPaymentId = "payment_12345";
    }

    @Test
    @DisplayName("Should send payment success response successfully")
    void shouldSendPaymentSuccessResponseSuccessfully() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        
        SendResult<String, PremiumSubscriptionResponseEvent> sendResult = mock(SendResult.class);
        var recordMetadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(123L);
        
        future.complete(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals("premium-subscription-response", topicCaptor.getValue());
        
        String expectedKey = testUserId.toString() + "_" + eventCaptor.getValue().getEventId();
        assertEquals(expectedKey, keyCaptor.getValue());
        
        PremiumSubscriptionResponseEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getEventId());
        assertEquals("PREMIUM_SUBSCRIPTION_RESPONSE", capturedEvent.getEventType());
        assertNotNull(capturedEvent.getTimestamp());
        assertEquals(testSubscriptionId, capturedEvent.getSubscriptionId());
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(testPaymentId, capturedEvent.getPaymentId());
        assertEquals("SUCCESS", capturedEvent.getStatus());
        assertEquals("Payment completed successfully", capturedEvent.getMessage());
        assertNull(capturedEvent.getConfirmationUrl());
    }

    @Test
    @DisplayName("Should handle Kafka send failure gracefully")
    void shouldHandleKafkaSendFailureGracefully() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        
        RuntimeException exception = new RuntimeException("Kafka send failed");
        future.completeExceptionally(exception);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        assertDoesNotThrow(() -> producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId));
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class));
    }

    @Test
    @DisplayName("Should handle critical Kafka template exception")
    void shouldHandleCriticalKafkaTemplateException() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenThrow(new RuntimeException("Kafka template error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);
        });
        
        assertEquals("Failed to send premium subscription response", exception.getMessage());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class));
    }

    @Test
    @DisplayName("Should generate correct key format")
    void shouldGenerateCorrectKeyFormat() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        String key = keyCaptor.getValue();
        UUID eventId = eventCaptor.getValue().getEventId();
        String expectedKey = testUserId.toString() + "_" + eventId;
        assertEquals(expectedKey, key);
    }

    @Test
    @DisplayName("Should use correct topic name")
    void shouldUseCorrectTopicName() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals("premium-subscription-response", topicCaptor.getValue());
    }

    @Test
    @DisplayName("Should send payment success response with different parameters")
    void shouldSendPaymentSuccessResponseWithDifferentParameters() throws Exception {
        UUID differentSubscriptionId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        String differentPaymentId = "payment_67890";

        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        producer.sendPaymentSuccessResponse(differentSubscriptionId, differentUserId, differentPaymentId);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        PremiumSubscriptionResponseEvent capturedEvent = eventCaptor.getValue();
        assertEquals(differentSubscriptionId, capturedEvent.getSubscriptionId());
        assertEquals(differentUserId, capturedEvent.getUserId());
        assertEquals(differentPaymentId, capturedEvent.getPaymentId());
        assertEquals("SUCCESS", capturedEvent.getStatus());
        assertEquals("Payment completed successfully", capturedEvent.getMessage());
    }

    @Test
    @DisplayName("Should create unique event IDs for each call")
    void shouldCreateUniqueEventIdsForEachCall() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class));
        
        ArgumentCaptor<PremiumSubscriptionResponseEvent> firstEventCaptor = ArgumentCaptor.forClass(PremiumSubscriptionResponseEvent.class);
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), firstEventCaptor.capture());
        UUID firstEventId = firstEventCaptor.getValue().getEventId();

        producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class));
        
        ArgumentCaptor<PremiumSubscriptionResponseEvent> secondEventCaptor = ArgumentCaptor.forClass(PremiumSubscriptionResponseEvent.class);
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), secondEventCaptor.capture());
        UUID secondEventId = secondEventCaptor.getValue().getEventId();

        assertNotEquals(firstEventId, secondEventId);
    }

    @Test
    @DisplayName("Should set timestamp to current time")
    void shouldSetTimestampToCurrentTime() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionResponseEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionResponseEvent.class)))
            .thenReturn(future);

        LocalDateTime beforeSend = LocalDateTime.now();
        producer.sendPaymentSuccessResponse(testSubscriptionId, testUserId, testPaymentId);
        LocalDateTime afterSend = LocalDateTime.now();

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        PremiumSubscriptionResponseEvent capturedEvent = eventCaptor.getValue();
        LocalDateTime eventTimestamp = capturedEvent.getTimestamp();
        
        assertNotNull(eventTimestamp);
        assertTrue(eventTimestamp.isAfter(beforeSend.minusSeconds(1)));
        assertTrue(eventTimestamp.isBefore(afterSend.plusSeconds(1)));
    }
}
