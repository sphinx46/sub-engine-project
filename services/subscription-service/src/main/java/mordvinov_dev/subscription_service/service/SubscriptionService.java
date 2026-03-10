package mordvinov_dev.subscription_service.service;

import mordvinov_dev.subscription_service.dto.request.CreateSubscriptionRequest;
import mordvinov_dev.subscription_service.dto.request.pageable.PageRequest;
import mordvinov_dev.subscription_service.dto.response.SubscriptionResponse;
import mordvinov_dev.subscription_service.dto.response.pageable.PageResponse;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(CreateSubscriptionRequest request, UUID userId, String userEmail);

    PageResponse<SubscriptionResponse> getUserSubscriptions(UUID userId, PageRequest pageRequest);

    PageResponse<SubscriptionResponse> getUserSubscriptionsByStatus(UUID userId, StatusType status, PageRequest pageRequest);

    SubscriptionResponse cancelSubscription(UUID subscriptionId, UUID userId);

    long getUserActiveSubscriptionsCount(UUID userId);

    SubscriptionResponse updateSubscriptionPlan(UUID subscriptionId, UUID userId, PlanType newPlan, String userEmail);
}