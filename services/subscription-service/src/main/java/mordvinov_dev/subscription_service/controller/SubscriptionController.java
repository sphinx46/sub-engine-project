package mordvinov_dev.subscription_service.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.dto.request.CreateSubscriptionRequest;
import mordvinov_dev.subscription_service.dto.response.SubscriptionResponse;
import mordvinov_dev.subscription_service.dto.response.pageable.PageResponse;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;
import mordvinov_dev.subscription_service.service.SubscriptionService;
import mordvinov_dev.subscription_service.dto.request.pageable.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@RequestBody CreateSubscriptionRequest request,
                                                                   @RequestHeader("X-User-Id") UUID userId) {
        SubscriptionResponse response = subscriptionService.createSubscription(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getUserSubscriptions(@PathVariable UUID userId,
                                                                                   @RequestParam(defaultValue = "20", required = false) @Min(1) final Integer size,
                                                                                   @RequestParam(defaultValue = "0", required = false) @Min(0) final Integer pageNumber,
                                                                                   @RequestParam(defaultValue = "createdAt", required = false) final String sortedBy,
                                                                                   @RequestParam(defaultValue = "DESC", required = false) final String direction) {

        var pageRequest = PageRequest.builder()
                .pageNumber(pageNumber)
                .size(size)
                .sortBy(sortedBy)
                .direction(Sort.Direction.fromString(direction))
                .build();

        PageResponse<SubscriptionResponse> response = subscriptionService.getUserSubscriptions(userId, pageRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/status/{status}")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getUserSubscriptionsByStatus(
            @PathVariable UUID userId,
            @PathVariable StatusType status,
            @RequestParam(defaultValue = "20", required = false) @Min(1) final Integer size,
            @RequestParam(defaultValue = "0", required = false) @Min(0) final Integer pageNumber,
            @RequestParam(defaultValue = "createdAt", required = false) final String sortedBy,
            @RequestParam(defaultValue = "DESC", required = false) final String direction) {

        var pageRequest = PageRequest.builder()
                .pageNumber(pageNumber)
                .size(size)
                .sortBy(sortedBy)
                .direction(Sort.Direction.fromString(direction))
                .build();

        PageResponse<SubscriptionResponse> response = subscriptionService.getUserSubscriptionsByStatus(userId, status, pageRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(@PathVariable UUID subscriptionId,
                                                                   @RequestHeader("X-User-Id") UUID userId) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/active/count")
    public ResponseEntity<Long> getUserActiveSubscriptionsCount(@PathVariable UUID userId) {
        long count = subscriptionService.getUserActiveSubscriptionsCount(userId);
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{subscriptionId}/plan")
    public ResponseEntity<SubscriptionResponse> updateSubscriptionPlan(@PathVariable UUID subscriptionId,
                                                                       @RequestHeader("X-User-Id") UUID userId,
                                                                       @RequestParam PlanType newPlan) {
        SubscriptionResponse response = subscriptionService.updateSubscriptionPlan(subscriptionId, userId, newPlan);
        return ResponseEntity.ok(response);
    }
}