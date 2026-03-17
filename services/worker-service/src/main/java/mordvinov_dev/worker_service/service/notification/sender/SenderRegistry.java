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

    /**
     * Constructs a SenderRegistry and registers all available notification senders.
     * 
     * @param senderList list of notification senders to register
     */
    public SenderRegistry(List<NotificationSender> senderList) {
        senderList.forEach(sender -> {
            senders.put(sender.getChannel(), sender);
            log.info("Registered sender: {}", sender.getChannel());
        });
    }

    /**
     * Retrieves the appropriate notification sender for the specified channel.
     * 
     * @param channel the notification channel to get a sender for
     * @return the notification sender for the specified channel
     * @throws IllegalArgumentException if no sender is found for the channel
     */
    public NotificationSender getSender(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender found for channel: " + channel);
        }
        return sender;
    }
}