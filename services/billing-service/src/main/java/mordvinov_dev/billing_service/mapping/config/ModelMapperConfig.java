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
import ru.loolzaaa.youkassa.model.Payment;

import java.time.Instant;
import java.util.UUID;


/**
 * Конфигурационный класс для настройки ModelMapper.
 * Определяет правила маппинга между сущностями, DTO и событиями.
 * Обеспечивает корректное преобразование данных между слоями приложения.
 *
 */
@Slf4j
@Configuration
public class ModelMapperConfig {

    /**
     * Создает и настраивает ModelMapper для преобразования сущностей в DTO и события.
     * Включает сопоставление полей, пропуск null значений и доступ к приватным полям.
     *
     * @return настроенный экземпляр ModelMapper с определенными правилами маппинга
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
                map().setCreatedAt(Instant.from(source.getCreatedAt()));
                map().setUpdatedAt(Instant.from(source.getUpdatedAt()));
            }
        });
    }

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
                map().setCreatedAt(Instant.from(source.getCreatedAt()));
                map().setUpdatedAt(Instant.from(source.getUpdatedAt()));
            }
        });
    }
}