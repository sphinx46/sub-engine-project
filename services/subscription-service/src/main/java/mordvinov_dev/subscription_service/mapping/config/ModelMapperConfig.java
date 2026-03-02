package mordvinov_dev.subscription_service.mapping.config;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.subscription_service.dto.response.SubscriptionResponse;
import mordvinov_dev.subscription_service.entity.Subscription;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


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
     * Настраивает маппинг между сущностью Message и MessageResponse.
     * Определяет правила преобразования полей сообщения в DTO ответа.
     *
     * @param modelMapper экземпляр ModelMapper для настройки
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