package mordvinov_dev.subscription_service.service;

import mordvinov_dev.subscription_service.dto.request.CreateSubscriptionRequest;
import mordvinov_dev.subscription_service.dto.request.pageable.PageRequest;
import mordvinov_dev.subscription_service.dto.response.SubscriptionResponse;
import mordvinov_dev.subscription_service.dto.response.pageable.PageResponse;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;

import java.util.UUID;

/**
 * Service interface for managing subscription operations.
 */
public interface SubscriptionService {

    /**
     * Creates a new subscription for the specified user.
     * @param request the subscription creation request
     * @param userId the unique identifier of the user
     * @param userEmail the email address of the user
     * @return the created subscription response
     */
    SubscriptionResponse createSubscription(CreateSubscriptionRequest request, UUID userId, String userEmail);

    /**
     * Retrieves all subscriptions for a specific user with pagination.
     * @param userId the unique identifier of the user
     * @param pageRequest the pagination parameters
     * @return a paginated list of user subscriptions
     */
    PageResponse<SubscriptionResponse> getUserSubscriptions(UUID userId, PageRequest pageRequest);

    /**
     * Retrieves user subscriptions filtered by status with pagination.
     * @param userId the unique identifier of the user
     * @param status the subscription status to filter by
     * @param pageRequest the pagination parameters
     * @return a paginated list of user subscriptions with the specified status
     */
    PageResponse<SubscriptionResponse> getUserSubscriptionsByStatus(UUID userId, StatusType status, PageRequest pageRequest);

    /**
     * Cancels a subscription for the specified user.
     * @param subscriptionId the unique identifier of the subscription
     * @param userId the unique identifier of the user
     * @return the cancelled subscription response
     */
    SubscriptionResponse cancelSubscription(UUID subscriptionId, UUID userId);

    /**
     * Counts the number of active subscriptions for a user.
     * @param userId the unique identifier of the user
     * @return the count of active subscriptions
     */
    long getUserActiveSubscriptionsCount(UUID userId);

    /**
     * Updates the subscription plan for a user.
     * @param subscriptionId the unique identifier of the subscription
     * @param userId the unique identifier of the user
     * @param newPlan the new plan type to upgrade to
     * @param userEmail the email address of the user
     * @return the updated subscription response
     */
    SubscriptionResponse updateSubscriptionPlan(UUID subscriptionId, UUID userId, PlanType newPlan, String userEmail);
}