package mordvinov_dev.subscription_service.validation;

import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.exception.SubscriptionAccessDeniedException;
import mordvinov_dev.subscription_service.exception.SubscriptionNotFoundException;
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionOwnershipValidatorTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionOwnershipValidator validator;

    @Test
    void validateAndGetSubscription_returnsSubscriptionWhenUserIsOwner() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(userId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        Subscription result = validator.validateAndGetSubscription(subscriptionId, userId);

        assertNotNull(result);
        assertEquals(subscriptionId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_throwsSubscriptionNotFoundExceptionWhenSubscriptionNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        SubscriptionNotFoundException exception = assertThrows(SubscriptionNotFoundException.class,
            () -> validator.validateAndGetSubscription(subscriptionId, userId));

        assertEquals("Subscription not found with id: " + subscriptionId, exception.getMessage());
        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_throwsSubscriptionAccessDeniedExceptionWhenUserIsNotOwner() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(differentUserId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        SubscriptionAccessDeniedException exception = assertThrows(SubscriptionAccessDeniedException.class,
            () -> validator.validateAndGetSubscription(subscriptionId, userId));

        assertEquals(String.format("User %s does not have access to subscription %s", userId, subscriptionId), 
                    exception.getMessage());
        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_throwsSubscriptionAccessDeniedExceptionWhenUserIdIsNull() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = null;
        UUID subscriptionUserId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(subscriptionUserId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        SubscriptionAccessDeniedException exception = assertThrows(SubscriptionAccessDeniedException.class,
            () -> validator.validateAndGetSubscription(subscriptionId, userId));

        assertEquals(String.format("User %s does not have access to subscription %s", userId, subscriptionId), 
                    exception.getMessage());
        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_throwsNullPointerExceptionWhenSubscriptionUserIdIsNull() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(null);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        assertThrows(NullPointerException.class,
            () -> validator.validateAndGetSubscription(subscriptionId, userId));

        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_throwsNullPointerExceptionWhenBothUserIdsAreNull() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = null;
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(null);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        assertThrows(NullPointerException.class,
            () -> validator.validateAndGetSubscription(subscriptionId, userId));

        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_verifiesRepositoryCalledOnce() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(userId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        validator.validateAndGetSubscription(subscriptionId, userId);

        verify(subscriptionRepository, times(1)).findById(subscriptionId);
        verifyNoMoreInteractions(subscriptionRepository);
    }

    @Test
    void validateAndGetSubscription_handlesSameUserIdReference() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(userId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        Subscription result = validator.validateAndGetSubscription(subscriptionId, userId);

        assertNotNull(result);
        assertSame(userId, result.getUserId());
        verify(subscriptionRepository).findById(subscriptionId);
    }

    @Test
    void validateAndGetSubscription_differentUserIdsWithSameValue() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.fromString(userId1.toString());
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUserId(userId1);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        Subscription result = validator.validateAndGetSubscription(subscriptionId, userId2);

        assertNotNull(result);
        assertEquals(userId1, result.getUserId());
        assertEquals(userId2, userId1);
        verify(subscriptionRepository).findById(subscriptionId);
    }
}
