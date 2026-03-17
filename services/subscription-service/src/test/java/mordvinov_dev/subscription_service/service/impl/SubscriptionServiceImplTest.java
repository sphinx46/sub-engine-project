package mordvinov_dev.subscription_service.service.impl;

import mordvinov_dev.subscription_service.client.BillingServiceClient;
import mordvinov_dev.subscription_service.dto.request.CreateSubscriptionRequest;
import mordvinov_dev.subscription_service.dto.request.pageable.PageRequest;
import mordvinov_dev.subscription_service.dto.response.SubscriptionResponse;
import mordvinov_dev.subscription_service.dto.response.pageable.PageResponse;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;
import mordvinov_dev.subscription_service.exception.InvalidSubscriptionStatusException;
import mordvinov_dev.subscription_service.exception.SubscriptionAlreadyCancelledException;
import mordvinov_dev.subscription_service.mapping.EntityMapper;
import mordvinov_dev.subscription_service.producer.PremiumSubscriptionProducer;
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
import mordvinov_dev.subscription_service.validation.SubscriptionOwnershipValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private EntityMapper entityMapper;

    @Mock
    private SubscriptionOwnershipValidator ownershipValidator;

    @Mock
    private PremiumSubscriptionProducer premiumSubscriptionProducer;

    @Mock
    private BillingServiceClient billingServiceClient;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void createSubscription_createsFreeSubscriptionSuccessfully() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanType(PlanType.FREE);
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(StatusType.ACTIVE);
        when(entityMapper.map(request, Subscription.class)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(new SubscriptionResponse());

        SubscriptionResponse response = subscriptionService.createSubscription(request, userId, "user@example.com");

        assertNotNull(response);
        verify(subscriptionRepository).save(subscription);
        assertEquals(StatusType.ACTIVE, subscription.getStatus());
        assertEquals(PlanType.FREE, subscription.getPlanType());
        assertNotNull(subscription.getNextBillingDate());
    }

    @Test
    void createSubscription_withNullPlanType_createsFreeSubscription() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanType(null);
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        when(entityMapper.map(request, Subscription.class)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(new SubscriptionResponse());

        SubscriptionResponse response = subscriptionService.createSubscription(request, userId, "user@example.com");

        assertNotNull(response);
        verify(subscriptionRepository).save(subscription);
        assertEquals(StatusType.ACTIVE, subscription.getStatus());
        assertEquals(PlanType.FREE, subscription.getPlanType());
        assertNotNull(subscription.getNextBillingDate());
    }

    @Test
    void createSubscription_createsPremiumSubscriptionWithPaymentSuccess() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanType(PlanType.PREMIUM);
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(StatusType.PENDING);
        SubscriptionResponse expectedResponse = new SubscriptionResponse();
        String confirmationUrl = "https://payment.example.com/confirm/123";
        
        when(entityMapper.map(request, Subscription.class)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(expectedResponse);
        when(billingServiceClient.createPayment(subscription.getId(), userId)).thenReturn(confirmationUrl);

        SubscriptionResponse response = subscriptionService.createSubscription(request, userId, "user@example.com");

        assertNotNull(response);
        assertEquals(expectedResponse, response);
        assertEquals(confirmationUrl, response.getConfirmationUrl());
        assertEquals("Subscription created. Please complete payment using the provided URL to activate.", response.getMessage());
        verify(subscriptionRepository).save(subscription);
        verify(billingServiceClient).createPayment(subscription.getId(), userId);
        verifyNoInteractions(premiumSubscriptionProducer);
    }

    @Test
    void createSubscription_handlesPaymentFailureForPremiumPlan() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanType(PlanType.PREMIUM);
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(StatusType.PENDING);
        when(entityMapper.map(request, Subscription.class)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(new SubscriptionResponse());
        doThrow(new RuntimeException("Payment error")).when(billingServiceClient).createPayment(subscription.getId(), userId);

        SubscriptionResponse response = subscriptionService.createSubscription(request, userId, "user@example.com");

        assertNotNull(response);
        assertEquals("Subscription created but payment initiation failed. You will receive payment link shortly.", response.getMessage());
        verify(premiumSubscriptionProducer).sendPremiumSubscriptionRequest(subscription, userId, "user@example.com");
    }

    @Test
    void updateSubscriptionPlan_updatesPlanSuccessfully() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlanType newPlan = PlanType.FREE;
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(StatusType.ACTIVE);
        subscription.setPlanType(PlanType.PREMIUM);
        
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(new SubscriptionResponse());

        SubscriptionResponse response = subscriptionService.updateSubscriptionPlan(subscriptionId, userId, newPlan, "user@example.com");

        assertNotNull(response);
        assertEquals(newPlan, subscription.getPlanType());
        assertNotNull(subscription.getUpdatedAt());
        verify(subscriptionRepository).save(subscription);
        verifyNoInteractions(billingServiceClient);
        verifyNoInteractions(premiumSubscriptionProducer);
    }

    @Test
    void updateSubscriptionPlan_updatesToPremiumWithPaymentSuccess() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlanType newPlan = PlanType.PREMIUM;
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(StatusType.ACTIVE);
        subscription.setPlanType(PlanType.FREE);
        SubscriptionResponse response = new SubscriptionResponse();
        String confirmationUrl = "https://payment.example.com/confirm/456";
        
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenAnswer(invocation -> {
            SubscriptionResponse mappedResponse = new SubscriptionResponse();
            mappedResponse.setId(subscription.getId());
            mappedResponse.setUserId(subscription.getUserId());
            mappedResponse.setPlanType(subscription.getPlanType());
            mappedResponse.setStatus(subscription.getStatus());
            mappedResponse.setNextBillingDate(subscription.getNextBillingDate());
            mappedResponse.setCreatedAt(subscription.getCreatedAt());
            mappedResponse.setUpdatedAt(subscription.getUpdatedAt());
            return mappedResponse;
        });
        when(billingServiceClient.createPayment(subscriptionId, userId)).thenReturn(confirmationUrl);

        SubscriptionResponse actualResponse = subscriptionService.updateSubscriptionPlan(subscriptionId, userId, newPlan, "user@example.com");

        assertNotNull(actualResponse);
        assertEquals(newPlan, subscription.getPlanType());
        assertEquals(StatusType.PENDING, subscription.getStatus());
        assertNull(subscription.getNextBillingDate());
        assertEquals(confirmationUrl, actualResponse.getConfirmationUrl());
        assertEquals(StatusType.PENDING, actualResponse.getStatus());
        assertEquals("Plan updated to PREMIUM. Please complete payment using the provided URL to activate.", actualResponse.getMessage());
        verify(subscriptionRepository, times(2)).save(subscription);
        verify(billingServiceClient).createPayment(subscriptionId, userId);
        verify(premiumSubscriptionProducer).sendPremiumSubscriptionRequest(subscription, userId, "user@example.com");
    }

    @Test
    void updateSubscriptionPlan_updatesToPremiumWithPaymentFailure() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlanType newPlan = PlanType.PREMIUM;
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(StatusType.ACTIVE);
        subscription.setPlanType(PlanType.FREE);
        SubscriptionResponse expectedResponse = new SubscriptionResponse();
        
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(expectedResponse);
        doThrow(new RuntimeException("Payment error")).when(billingServiceClient).createPayment(subscriptionId, userId);

        SubscriptionResponse response = subscriptionService.updateSubscriptionPlan(subscriptionId, userId, newPlan, "user@example.com");

        assertNotNull(response);
        assertEquals(newPlan, subscription.getPlanType());
        assertEquals(StatusType.PENDING, subscription.getStatus());
        assertEquals("Plan updated to PREMIUM but payment initiation failed. Please try again later.", response.getMessage());
        verify(subscriptionRepository, times(2)).save(subscription);
        verify(billingServiceClient).createPayment(subscriptionId, userId);
        verify(premiumSubscriptionProducer).sendPremiumSubscriptionRequest(subscription, userId, "user@example.com");
    }

    @Test
    void updateSubscriptionPlan_throwsExceptionForNonActiveSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlanType newPlan = PlanType.PREMIUM;
        Subscription subscription = new Subscription();
        subscription.setStatus(StatusType.PENDING);
        
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);

        assertThrows(InvalidSubscriptionStatusException.class, 
            () -> subscriptionService.updateSubscriptionPlan(subscriptionId, userId, newPlan, "user@example.com"));
    }

    @Test
    void getUserSubscriptions_returnsPageResponse() {
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.builder()
            .pageNumber(0)
            .size(10)
            .sortBy("createdAt")
            .direction(Sort.Direction.DESC)
            .build();
        Subscription subscription1 = new Subscription();
        Subscription subscription2 = new Subscription();
        List<Subscription> subscriptions = List.of(subscription1, subscription2);
        Page<Subscription> subscriptionPage = new PageImpl<>(subscriptions, pageRequest.toPageable(), 2);
        
        when(subscriptionRepository.findAllByUserId(userId, pageRequest.toPageable())).thenReturn(subscriptionPage);
        when(entityMapper.mapList(subscriptions, SubscriptionResponse.class)).thenReturn(List.of(new SubscriptionResponse(), new SubscriptionResponse()));

        PageResponse<SubscriptionResponse> response = subscriptionService.getUserSubscriptions(userId, pageRequest);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());
        assertEquals(10, response.getPageSize());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void getUserSubscriptionsByStatus_returnsPageResponse() {
        UUID userId = UUID.randomUUID();
        StatusType status = StatusType.ACTIVE;
        PageRequest pageRequest = PageRequest.builder()
            .pageNumber(0)
            .size(5)
            .sortBy("createdAt")
            .direction(Sort.Direction.ASC)
            .build();
        Subscription subscription = new Subscription();
        List<Subscription> subscriptions = List.of(subscription);
        Page<Subscription> subscriptionPage = new PageImpl<>(subscriptions, pageRequest.toPageable(), 1);
        
        when(subscriptionRepository.findAllByUserIdAndStatus(userId, status, pageRequest.toPageable())).thenReturn(subscriptionPage);
        when(entityMapper.mapList(subscriptions, SubscriptionResponse.class)).thenReturn(List.of(new SubscriptionResponse()));

        PageResponse<SubscriptionResponse> response = subscriptionService.getUserSubscriptionsByStatus(userId, status, pageRequest);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(1, response.getTotalElements());
        assertEquals(5, response.getPageSize());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void getUserActiveSubscriptionsCount_returnsCorrectCount() {
        UUID userId = UUID.randomUUID();
        long expectedCount = 3L;
        
        when(subscriptionRepository.countByUserIdAndStatus(userId, StatusType.ACTIVE)).thenReturn(expectedCount);

        long count = subscriptionService.getUserActiveSubscriptionsCount(userId);

        assertEquals(expectedCount, count);
        verify(subscriptionRepository).countByUserIdAndStatus(userId, StatusType.ACTIVE);
    }

    @Test
    void cancelSubscription_throwsExceptionWhenAlreadyCancelled() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setStatus(StatusType.CANCELLED);
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);

        assertThrows(SubscriptionAlreadyCancelledException.class, () -> subscriptionService.cancelSubscription(subscriptionId, userId));
    }

    @Test
    void cancelSubscription_cancelsActiveSubscriptionSuccessfully() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setStatus(StatusType.ACTIVE);
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(new SubscriptionResponse());

        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId, userId);

        assertNotNull(response);
        assertEquals(StatusType.CANCELLED, subscription.getStatus());
        assertNotNull(subscription.getUpdatedAt());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscription_cancelsPendingSubscriptionSuccessfully() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setStatus(StatusType.PENDING);
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(entityMapper.map(subscription, SubscriptionResponse.class)).thenReturn(new SubscriptionResponse());

        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId, userId);

        assertNotNull(response);
        assertEquals(StatusType.CANCELLED, subscription.getStatus());
        assertNotNull(subscription.getUpdatedAt());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscription_throwsExceptionForFailedSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setStatus(StatusType.FAILED);
        when(ownershipValidator.validateAndGetSubscription(subscriptionId, userId)).thenReturn(subscription);

        assertThrows(InvalidSubscriptionStatusException.class, 
            () -> subscriptionService.cancelSubscription(subscriptionId, userId));
    }

    @Test
    void activatePremiumSubscription_throwsExceptionWhenSubscriptionNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> subscriptionService.activatePremiumSubscription(subscriptionId));
        assertEquals("404 NOT_FOUND \"Subscription not found\"", exception.getMessage());
    }

    @Test
    void activatePremiumSubscription_activatesSubscriptionSuccessfully() {
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setStatus(StatusType.PENDING);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        subscriptionService.activatePremiumSubscription(subscriptionId);

        assertEquals(StatusType.ACTIVE, subscription.getStatus());
        assertNotNull(subscription.getNextBillingDate());
        assertNotNull(subscription.getUpdatedAt());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void failPremiumSubscription_throwsExceptionWhenSubscriptionNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> subscriptionService.failPremiumSubscription(subscriptionId));
        assertEquals("404 NOT_FOUND \"Subscription not found\"", exception.getMessage());
    }

    @Test
    void failPremiumSubscription_marksSubscriptionAsFailed() {
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        subscriptionService.failPremiumSubscription(subscriptionId);

        assertEquals(StatusType.FAILED, subscription.getStatus());
        assertNotNull(subscription.getUpdatedAt());
        verify(subscriptionRepository).save(subscription);
    }
}