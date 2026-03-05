package mordvinov_dev.billing_service.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final String PREMIUM_SUBSCRIPTION_REQUEST_TOPIC = "premium-subscription-request";
    private static final String PREMIUM_SUBSCRIPTION_RESPONSE_TOPIC = "premium-subscription-response";
    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    public NewTopic premiumSubscriptionRequestTopic() {
        return TopicBuilder.name(PREMIUM_SUBSCRIPTION_REQUEST_TOPIC)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic premiumSubscriptionResponseTopic() {
        return TopicBuilder.name(PREMIUM_SUBSCRIPTION_RESPONSE_TOPIC)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}