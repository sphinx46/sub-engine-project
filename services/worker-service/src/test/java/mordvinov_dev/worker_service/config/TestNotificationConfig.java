package mordvinov_dev.worker_service.config;

import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.service.notification.sender.NotificationSender;
import mordvinov_dev.worker_service.service.notification.sender.SenderRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestNotificationConfig {

    @Bean
    @Primary
    public NotificationSender mockEmailSender() {
        NotificationSender mockSender = Mockito.mock(NotificationSender.class);
        when(mockSender.send(any())).thenReturn(
                NotificationResponse.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .success(true)
                        .message("Test success")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
        when(mockSender.getChannel()).thenReturn(mordvinov_dev.worker_service.domain.document.enums.NotificationChannel.EMAIL);
        return mockSender;
    }

    @Bean
    @Primary
    public SenderRegistry testSenderRegistry(NotificationSender mockEmailSender) {
        return new SenderRegistry(List.of(mockEmailSender));
    }
}