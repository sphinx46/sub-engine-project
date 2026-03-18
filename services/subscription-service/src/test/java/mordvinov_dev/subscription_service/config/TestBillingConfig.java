package mordvinov_dev.subscription_service.config;

import mordvinov_dev.subscription_service.client.BillingServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestConfiguration
@Profile("test")
public class TestBillingConfig {

    @Bean
    @Primary
    public BillingServiceClient mockBillingServiceClient() {
        BillingServiceClient mock = org.mockito.Mockito.mock(BillingServiceClient.class);
        when(mock.createPayment(any(UUID.class), any(UUID.class)))
                .thenReturn("https://test-payment-url.com/confirm/");
        return mock;
    }
}