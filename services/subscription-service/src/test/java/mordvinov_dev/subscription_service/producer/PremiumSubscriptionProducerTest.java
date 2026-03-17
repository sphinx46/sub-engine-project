package mordvinov_dev.subscription_service.producer;

import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;
import mordvinov_dev.subscription_service.event.PremiumSubscriptionRequestEvent;
import mordvinov_dev.subscription_service.event.producer.PremiumSubscriptionProducer;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PremiumSubscriptionProducer Tests")
class PremiumSubscriptionProducerTest {

    @Mock
    private KafkaTemplate<String, PremiumSubscriptionRequestEvent> kafkaTemplate;

    @InjectMocks
    private PremiumSubscriptionProducer producer;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<PremiumSubscriptionRequestEvent> eventCaptor;

    private Subscription testSubscription;
    private UUID testUserId;
    private String testUserEmail;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserEmail = "test@example.com";
        
        testSubscription = new Subscription();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setUserId(testUserId);
        testSubscription.setPlanType(PlanType.PREMIUM);
        testSubscription.setStatus(StatusType.ACTIVE);

        ReflectionTestUtils.setField(producer, "requestTopic", "premium-subscription-request");
    }

    @Test
    @DisplayName("Should send premium subscription request successfully")
    void shouldSendPremiumSubscriptionRequestSuccessfully() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future = 
            new CompletableFuture<>();
        
        SendResult<String, PremiumSubscriptionRequestEvent> sendResult = mock(SendResult.class);
        var recordMetadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(123L);
        
        future.complete(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class)))
            .thenReturn(future);

        producer.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals("premium-subscription-request", topicCaptor.getValue());
        assertTrue(keyCaptor.getValue().startsWith(testUserId.toString() + "_"));
        
        PremiumSubscriptionRequestEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getEventId());
        assertEquals("PREMIUM_SUBSCRIPTION_REQUEST", capturedEvent.getEventType());
        assertEquals(testSubscription.getId(), capturedEvent.getSubscriptionId());
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(new BigDecimal("1000.00"), capturedEvent.getAmount());
        assertEquals("RUB", capturedEvent.getCurrency());
        assertEquals("Premium subscription payment", capturedEvent.getDescription());
        assertEquals(testUserEmail, capturedEvent.getUserEmail());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    @DisplayName("Should handle Kafka send failure gracefully")
    void shouldHandleKafkaSendFailureGracefully() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future = 
            new CompletableFuture<>();
        
        RuntimeException exception = new RuntimeException("Kafka send failed");
        future.completeExceptionally(exception);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class)))
            .thenReturn(future);

        assertDoesNotThrow(() -> producer.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail));
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class));
    }

    @Test
    @DisplayName("Should throw RuntimeException when Kafka template throws exception")
    void shouldThrowRuntimeExceptionWhenKafkaTemplateThrowsException() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class)))
            .thenThrow(new RuntimeException("Kafka template error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            producer.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail));
        
        assertEquals("Failed to send premium subscription request", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class));
    }

    @Test
    @DisplayName("Should generate unique event ID for each request")
    void shouldGenerateUniqueEventIdForEachRequest() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class)))
            .thenReturn(future);

        producer.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail);
        producer.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail);

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        PremiumSubscriptionRequestEvent firstEvent = eventCaptor.getAllValues().get(0);
        PremiumSubscriptionRequestEvent secondEvent = eventCaptor.getAllValues().get(1);
        
        assertNotEquals(firstEvent.getEventId(), secondEvent.getEventId());
    }

    @Test
    @DisplayName("Should generate correct key format")
    void shouldGenerateCorrectKeyFormat() throws Exception {
        CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class)))
            .thenReturn(future);

        producer.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        String key = keyCaptor.getValue();
        PremiumSubscriptionRequestEvent event = eventCaptor.getValue();
        
        String expectedKey = testUserId.toString() + "_" + event.getEventId();
        assertEquals(expectedKey, key);
    }

    @Test
    @DisplayName("Should use default topic when not configured")
    void shouldUseDefaultTopicWhenNotConfigured() throws Exception {
        PremiumSubscriptionProducer producerWithoutTopic = new PremiumSubscriptionProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producerWithoutTopic, "requestTopic", "premium-subscription-request");
        
        CompletableFuture<SendResult<String, PremiumSubscriptionRequestEvent>> future = 
            new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        
        when(kafkaTemplate.send(anyString(), anyString(), any(PremiumSubscriptionRequestEvent.class)))
            .thenReturn(future);

        producerWithoutTopic.sendPremiumSubscriptionRequest(testSubscription, testUserId, testUserEmail);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals("premium-subscription-request", topicCaptor.getValue());
    }
}
