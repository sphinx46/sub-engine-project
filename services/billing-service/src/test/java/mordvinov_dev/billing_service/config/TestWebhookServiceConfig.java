package mordvinov_dev.billing_service.config;

import mordvinov_dev.billing_service.service.WebhookService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestWebhookServiceConfig {

    @Bean
    @Primary
    public WebhookService mockWebhookService() {
        return org.mockito.Mockito.mock(WebhookService.class);
    }
}
