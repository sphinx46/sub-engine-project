package mordvinov_dev.billing_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import ru.loolzaaa.youkassa.client.ApiClient;

@TestConfiguration
@Profile("test")
public class TestYooKassaConfig {

    @Bean
    @Primary
    public ApiClient mockYooKassaApiClient() {
        return org.mockito.Mockito.mock(ApiClient.class);
    }
}
