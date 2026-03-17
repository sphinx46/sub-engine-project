package mordvinov_dev.subscription_service.mapping.config;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.dto.response.SubscriptionResponse;
import mordvinov_dev.subscription_service.entity.Subscription;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration class for ModelMapper setup and custom mappings.
 */
@Slf4j
@Configuration
public class ModelMapperConfig {

    /**
     * Creates and configures a ModelMapper bean with custom mappings.
     * @return configured ModelMapper instance
     */
    @Bean
    public ModelMapper modelMapper() {
        log.info("MODEL_MAPPER_КОНФИГУРАЦИЯ_НАЧАЛО");

        final ModelMapper modelMapper = new ModelMapper();

        configureSubscriptionMappings(modelMapper);

        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true)
                .setFieldAccessLevel(
                        org.modelmapper.config.Configuration
                                .AccessLevel.PRIVATE);

        log.info("MODEL_MAPPER_КОНФИГУРАЦИЯ_УСПЕХ");
        return modelMapper;
    }


    /**
     * Configures custom mappings for Subscription entity to SubscriptionResponse DTO.
     * @param modelMapper the ModelMapper instance to configure
     */
    private void configureSubscriptionMappings(final ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<Subscription, SubscriptionResponse>() {
            @Override
            protected void configure() {
                map().setUserId(source.getUserId());
                map().setNextBillingDate(source.getNextBillingDate());
                map().setStatus(source.getStatus());
                map().setPlanType(source.getPlanType());
                map().setCreatedAt(source.getCreatedAt());
                map().setUpdatedAt(source.getUpdatedAt());
            }
        });
    }
}