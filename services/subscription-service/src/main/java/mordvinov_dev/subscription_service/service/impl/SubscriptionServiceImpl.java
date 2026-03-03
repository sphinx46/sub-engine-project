package mordvinov_dev.subscription_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
import mordvinov_dev.subscription_service.service.SubscriptionService;
import mordvinov_dev.subscription_service.validation.SubscriptionOwnershipValidator;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EntityMapper entityMapper;
    private final SubscriptionOwnershipValidator ownershipValidator;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request, UUID userId) {
        log.info("Creating subscription for user: {}", userId);

        Subscription subscription = entityMapper.map(request, Subscription.class);
        subscription.setUserId(userId);
        subscription.setPlanType(request.getPlanType() != null ? request.getPlanType() : PlanType.FREE);
        subscription.setStatus(StatusType.ACTIVE);
        subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription created with id: {}", savedSubscription.getId());

        return entityMapper.map(savedSubscription, SubscriptionResponse.class);
    }

    @Override
    public PageResponse<SubscriptionResponse> getUserSubscriptions(UUID userId, PageRequest pageRequest) {
        log.debug("Fetching subscriptions for user: {}, page: {}", userId, pageRequest.getPageNumber());

        Page<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId, pageRequest.toPageable());

        return PageResponse.<SubscriptionResponse>builder()
                .content(entityMapper.mapList(subscriptions.getContent(), SubscriptionResponse.class))
                .currentPage(subscriptions.getNumber())
                .totalPages(subscriptions.getTotalPages())
                .totalElements(subscriptions.getTotalElements())
                .pageSize(subscriptions.getSize())
                .first(subscriptions.isFirst())
                .last(subscriptions.isLast())
                .build();
    }

    @Override
    public PageResponse<SubscriptionResponse> getUserSubscriptionsByStatus(UUID userId, StatusType status, PageRequest pageRequest) {
        log.debug("Fetching {} subscriptions for user: {}, page: {}", status, userId, pageRequest.getPageNumber());

        Page<Subscription> subscriptions = subscriptionRepository.findAllByUserIdAndStatus(userId, status, pageRequest.toPageable());

        return PageResponse.<SubscriptionResponse>builder()
                .content(entityMapper.mapList(subscriptions.getContent(), SubscriptionResponse.class))
                .currentPage(subscriptions.getNumber())
                .totalPages(subscriptions.getTotalPages())
                .totalElements(subscriptions.getTotalElements())
                .pageSize(subscriptions.getSize())
                .first(subscriptions.isFirst())
                .last(subscriptions.isLast())
                .build();
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID subscriptionId, UUID userId) {
        log.info("Cancelling subscription: {} for user: {}", subscriptionId, userId);

        Subscription subscription = ownershipValidator.validateAndGetSubscription(subscriptionId, userId);

        if (subscription.getStatus() == StatusType.CANCELLED) {
            throw new SubscriptionAlreadyCancelledException(subscriptionId);
        }

        if (subscription.getStatus() != StatusType.ACTIVE) {
            throw new InvalidSubscriptionStatusException(subscriptionId, subscription.getStatus(), StatusType.ACTIVE);
        }

        subscription.setStatus(StatusType.CANCELLED);
        subscription.setUpdatedAt(LocalDateTime.now());

        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription cancelled: {}", subscriptionId);

        return entityMapper.map(updatedSubscription, SubscriptionResponse.class);
    }

    @Override
    public long getUserActiveSubscriptionsCount(UUID userId) {
        log.debug("Counting active subscriptions for user: {}", userId);
        return subscriptionRepository.countByUserIdAndStatus(userId, StatusType.ACTIVE);
    }

    @Override
    @Transactional
    public SubscriptionResponse updateSubscriptionPlan(UUID subscriptionId, UUID userId, PlanType newPlan) {
        log.info("Updating subscription plan: {} for user: {} to: {}", subscriptionId, userId, newPlan);

        Subscription subscription = ownershipValidator.validateAndGetSubscription(subscriptionId, userId);

        if (subscription.getStatus() != StatusType.ACTIVE) {
            throw new InvalidSubscriptionStatusException(subscriptionId, subscription.getStatus(), StatusType.ACTIVE);
        }

        subscription.setPlanType(newPlan);
        subscription.setUpdatedAt(LocalDateTime.now());

        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription plan updated: {}", subscriptionId);

        return entityMapper.map(updatedSubscription, SubscriptionResponse.class);
    }
}