package mordvinov_dev.worker_service.notification.registry;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.notification.domain.NotificationType;
import mordvinov_dev.worker_service.notification.strategy.NotificationStrategy;
import mordvinov_dev.worker_service.notification.exception.NotificationStrategyNotFoundException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NotificationStrategyRegistry {

    private final Map<NotificationType, NotificationStrategy> strategies = new EnumMap<>(NotificationType.class);

    public NotificationStrategyRegistry(List<NotificationStrategy> strategyList) {
        strategyList.forEach(strategy -> {
            strategies.put(strategy.getType(), strategy);
            log.info("Registered notification strategy: {}", strategy.getType());
        });
    }

    public NotificationStrategy getStrategy(NotificationType type) {
        NotificationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            log.error("No strategy found for notification type: {}", type);
            throw new NotificationStrategyNotFoundException(type);
        }
        return strategy;
    }
}