package mordvinov_dev.subscription_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import mordvinov_dev.subscription_service.service.SubscriptionService;
import mordvinov_dev.subscription_service.validation.SubscriptionOwnershipValidator;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final PremiumSubscriptionProducer premiumSubscriptionProducer;
    private final BillingServiceClient billingServiceClient;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request, UUID userId, String userEmail) {
        log.info("Creating subscription for user: {}, planType: {}", userId, request.getPlanType());

        Subscription subscription = entityMapper.map(request, Subscription.class);
        subscription.setUserId(userId);
        subscription.setPlanType(request.getPlanType() != null ? request.getPlanType() : PlanType.FREE);

        if (request.getPlanType() == PlanType.PREMIUM) {
            subscription.setStatus(StatusType.PENDING);
            subscription.setNextBillingDate(null);
        } else {
            subscription.setStatus(StatusType.ACTIVE);
            subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        }

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription saved with id: {}, status: {}", savedSubscription.getId(), savedSubscription.getStatus());

        SubscriptionResponse response = entityMapper.map(savedSubscription, SubscriptionResponse.class);

        if (request.getPlanType() == PlanType.PREMIUM) {
            try {
                log.info("Creating payment for premium subscription: {}, user: {}", savedSubscription.getId(), userId);
                String confirmationUrl = billingServiceClient.createPayment(savedSubscription.getId(), userId);
                response.setConfirmationUrl(confirmationUrl);
                response.setMessage("Subscription created. Please complete payment using the provided URL to activate.");
                log.info("Payment created successfully for subscription: {}", savedSubscription.getId());

            } catch (Exception e) {
                log.error("Failed to create payment for subscription: {}, user: {}, error: {}",
                        savedSubscription.getId(), userId, e.getMessage(), e);
                log.info("Sending async premium subscription request as fallback for subscription: {}", savedSubscription.getId());
                premiumSubscriptionProducer.sendPremiumSubscriptionRequest(savedSubscription, userId, userEmail);
                response.setMessage("Subscription created but payment initiation failed. You will receive payment link shortly.");
            }
        }

        return response;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse updateSubscriptionPlan(UUID subscriptionId, UUID userId, PlanType newPlan, String userEmail) {
        log.info("Updating subscription plan: {} for user: {} to: {}", subscriptionId, userId, newPlan);

        Subscription subscription = ownershipValidator.validateAndGetSubscription(subscriptionId, userId);

        if (subscription.getStatus() != StatusType.ACTIVE) {
            log.warn("Invalid status for plan update: subscription={}, currentStatus={}, requiredStatus={}, user={}",
                    subscriptionId, subscription.getStatus(), StatusType.ACTIVE, userId);
            throw new InvalidSubscriptionStatusException(subscriptionId, subscription.getStatus(), StatusType.ACTIVE);
        }

        subscription.setPlanType(newPlan);
        subscription.setUpdatedAt(LocalDateTime.now());

        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription plan updated: {}, user: {}, newPlan: {}", subscriptionId, userId, newPlan);

        SubscriptionResponse response = entityMapper.map(updatedSubscription, SubscriptionResponse.class);

        if (newPlan == PlanType.PREMIUM) {
            subscription.setStatus(StatusType.PENDING);
            subscription.setNextBillingDate(null);
            updatedSubscription = subscriptionRepository.save(subscription);
            log.info("Subscription status set to PENDING for premium upgrade: {}, user: {}", subscriptionId, userId);

            response = entityMapper.map(updatedSubscription, SubscriptionResponse.class);

            try {
                log.info("Initiating synchronous payment creation for premium upgrade: {}, user: {}", subscriptionId, userId);
                String confirmationUrl = billingServiceClient.createPayment(subscriptionId, userId);
                response.setConfirmationUrl(confirmationUrl);
                response.setMessage("Plan updated to PREMIUM. Please complete payment using the provided URL to activate.");
                log.info("Payment initiated successfully for premium upgrade: {}, confirmationUrl obtained", subscriptionId);
            } catch (Exception e) {
                log.error("Failed to create payment for premium upgrade: {}, user: {}, error: {}",
                        subscriptionId, userId, e.getMessage(), e);
                response.setMessage("Plan updated to PREMIUM but payment initiation failed. Please try again later.");
            }

            log.info("Sending async premium subscription request to Kafka for upgrade: {}", subscriptionId);
            premiumSubscriptionProducer.sendPremiumSubscriptionRequest(updatedSubscription, userId, userEmail);
        }

        return response;
    }

    /** {@inheritDoc} */
    @Override
    public PageResponse<SubscriptionResponse> getUserSubscriptions(UUID userId, PageRequest pageRequest) {
        log.debug("Fetching subscriptions for user: {}, page: {}, size: {}", userId, pageRequest.getPageNumber(), pageRequest.getSize());

        Page<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId, pageRequest.toPageable());
        log.debug("Found {} total subscriptions for user: {}", subscriptions.getTotalElements(), userId);

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

    /** {@inheritDoc} */
    @Override
    public PageResponse<SubscriptionResponse> getUserSubscriptionsByStatus(UUID userId, StatusType status, PageRequest pageRequest) {
        log.debug("Fetching {} subscriptions for user: {}, page: {}, size: {}", status, userId, pageRequest.getPageNumber(), pageRequest.getSize());

        Page<Subscription> subscriptions = subscriptionRepository.findAllByUserIdAndStatus(userId, status, pageRequest.toPageable());
        log.debug("Found {} total {} subscriptions for user: {}", subscriptions.getTotalElements(), status, userId);

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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID subscriptionId, UUID userId) {
        log.info("Cancelling subscription: {} for user: {}", subscriptionId, userId);

        Subscription subscription = ownershipValidator.validateAndGetSubscription(subscriptionId, userId);

        if (subscription.getStatus() == StatusType.CANCELLED) {
            log.warn("Attempt to cancel already cancelled subscription: {}, user: {}", subscriptionId, userId);
            throw new SubscriptionAlreadyCancelledException(subscriptionId);
        }

        if (subscription.getStatus() != StatusType.ACTIVE && subscription.getStatus() != StatusType.PENDING) {
            log.warn("Invalid status for cancellation: subscription={}, currentStatus={}, requiredStatus={}, user={}",
                    subscriptionId, subscription.getStatus(), StatusType.ACTIVE, userId);
            throw new InvalidSubscriptionStatusException(subscriptionId, subscription.getStatus(), StatusType.ACTIVE);
        }

        subscription.setStatus(StatusType.CANCELLED);
        subscription.setUpdatedAt(LocalDateTime.now());

        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription cancelled successfully: {}, user: {}", subscriptionId, userId);

        return entityMapper.map(updatedSubscription, SubscriptionResponse.class);
    }

    /** {@inheritDoc} */
    @Override
    public long getUserActiveSubscriptionsCount(UUID userId) {
        log.debug("Counting active subscriptions for user: {}", userId);
        long count = subscriptionRepository.countByUserIdAndStatus(userId, StatusType.ACTIVE);
        log.debug("User: {} has {} active subscriptions", userId, count);
        return count;
    }


    /** {@inheritDoc} */
    @Transactional
    public void activatePremiumSubscription(UUID subscriptionId) {
        log.info("Activating premium subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    log.error("Subscription not found for activation: {}", subscriptionId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
                });

        subscription.setStatus(StatusType.ACTIVE);
        subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("Subscription {} activated successfully", subscriptionId);
    }

    /** {@inheritDoc} */
    @Transactional
    public void failPremiumSubscription(UUID subscriptionId) {
        log.info("Failing premium subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    log.error("Subscription not found for fail: {}", subscriptionId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found");
                });

        subscription.setStatus(StatusType.FAILED);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("Subscription {} marked as FAILED", subscriptionId);
    }
}