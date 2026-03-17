package mordvinov_dev.billing_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;

/**
 * Configuration class for YooKassa payment integration.
 * Sets up API client and configuration properties for YooKassa service.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "youkassa")
public class YooKassaConfig {

    private String shopId;
    private String secretKey;
    private String returnUrl;
    private String webhookUrl;

    /**
     * Creates and configures YooKassa API client.
     * @return configured ApiClient instance
     */
    @Bean
    public ApiClient yooKassaApiClient() {
        return ApiClientBuilder.newBuilder()
                .configureBasicAuth(shopId, secretKey)
                .build();
    }
}