package mordvinov_dev.worker_service.service.notification.sender;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SenderRegistry {

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public SenderRegistry(List<NotificationSender> senderList) {
        senderList.forEach(sender -> {
            senders.put(sender.getChannel(), sender);
            log.info("Registered sender: {}", sender.getChannel());
        });
    }

    public NotificationSender getSender(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender found for channel: " + channel);
        }
        return sender;
    }
}