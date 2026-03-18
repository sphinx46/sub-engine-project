package mordvinov_dev.billing_service.config.kafka;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.event.PremiumSubscriptionRequestEvent;
import mordvinov_dev.billing_service.event.PremiumSubscriptionResponseEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:billing-service-group}")
    private String groupId;

    private Map<String, Object> consumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        config.put(JsonDeserializer.TRUSTED_PACKAGES, "mordvinov_dev.billing_service.event");
        config.put(JsonDeserializer.TYPE_MAPPINGS,
                "premiumSubscriptionRequest:mordvinov_dev.billing_service.event.PremiumSubscriptionRequestEvent");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PremiumSubscriptionRequestEvent.class.getName());

        return config;
    }

    @Bean
    public ConsumerFactory<String, PremiumSubscriptionRequestEvent> premiumSubscriptionRequestConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerConfig(),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(PremiumSubscriptionRequestEvent.class))
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PremiumSubscriptionRequestEvent>
    premiumSubscriptionRequestKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PremiumSubscriptionRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(premiumSubscriptionRequestConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}