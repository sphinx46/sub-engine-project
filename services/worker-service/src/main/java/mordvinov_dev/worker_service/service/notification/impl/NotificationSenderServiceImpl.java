package mordvinov_dev.worker_service.service.notification.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import mordvinov_dev.worker_service.service.notification.registry.NotificationStrategyRegistry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSenderServiceImpl {

    private final NotificationStrategyRegistry strategyRegistry;

    public NotificationResult send(NotificationRequest request) {
        log.info("Sending notification via {} to: {}", request.getType(), request.getRecipient());

        var strategy = strategyRegistry.getStrategy(request.getType());
        return strategy.send(request);
    }
}