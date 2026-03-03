package mordvinov_dev.billing_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;

@Data
@Configuration
@ConfigurationProperties(prefix = "youkassa")
public class YooKassaConfig {

    private String shopId;
    private String secretKey;

    @Bean
    public ApiClient yooKassaApiClient() {
        return ApiClientBuilder.newBuilder()
                .configureBasicAuth(shopId, secretKey)
                .build();
    }
}