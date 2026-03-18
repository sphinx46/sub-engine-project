package mordvinov_dev.billing_service.mapping.config;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.entity.RefundEntity;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Configuration class for ModelMapper bean.
 * Sets up custom mappings between entities and DTOs.
 */
@Slf4j
@Configuration
public class ModelMapperConfig {

    /**
     * Creates and configures a ModelMapper bean.
     * @return configured ModelMapper instance
     */
    @Bean
    public ModelMapper modelMapper() {
        log.info("MODEL_MAPPER_КОНФИГУРАЦИЯ_НАЧАЛО");

        final ModelMapper modelMapper = new ModelMapper();

        configurePaymentEntityMappings(modelMapper);
        configureRefundMappings(modelMapper);

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
     * Configures mappings for RefundEntity to RefundResponse.
     * @param modelMapper the ModelMapper instance to configure
     */
    private void configureRefundMappings(final ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<RefundEntity, RefundResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setPaymentId(source.getPaymentId());
                map().setUserId(source.getUserId());
                map().setRefundId(source.getRefundId());
                map().setStatus(source.getStatus());
                map().setAmount(source.getAmount());
                map().setCurrency(source.getCurrency());
                map().setDescription(source.getDescription());

                using(ctx -> {
                    LocalDateTime dateTime = (LocalDateTime) ctx.getSource();
                    return dateTime != null ? dateTime.toInstant(ZoneOffset.UTC) : null;
                }).map(source.getCreatedAt()).setCreatedAt(null);

                using(ctx -> {
                    LocalDateTime dateTime = (LocalDateTime) ctx.getSource();
                    return dateTime != null ? dateTime.toInstant(ZoneOffset.UTC) : null;
                }).map(source.getUpdatedAt()).setUpdatedAt(null);
            }
        });
    }

    /**
     * Configures mappings for PaymentEntity to PaymentResponse.
     * @param modelMapper the ModelMapper instance to configure
     */
    private void configurePaymentEntityMappings(final ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<PaymentEntity, PaymentResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setPaymentId(source.getPaymentId());
                map().setUserId(source.getUserId());
                map().setSubscriptionId(source.getSubscriptionId());
                map().setStatus(source.getStatus());
                map().setAmount(source.getAmount());
                map().setCurrency(source.getCurrency());
                map().setDescription(source.getDescription());
                map().setPaymentMethodId(source.getPaymentMethodId());
                map().setPaymentMethodType(source.getPaymentMethodType());

                using(ctx -> {
                    LocalDateTime dateTime = (LocalDateTime) ctx.getSource();
                    return dateTime != null ? dateTime.toInstant(ZoneOffset.UTC) : null;
                }).map(source.getCreatedAt()).setCreatedAt(null);

                using(ctx -> {
                    LocalDateTime dateTime = (LocalDateTime) ctx.getSource();
                    return dateTime != null ? dateTime.toInstant(ZoneOffset.UTC) : null;
                }).map(source.getUpdatedAt()).setUpdatedAt(null);
            }
        });
    }
}