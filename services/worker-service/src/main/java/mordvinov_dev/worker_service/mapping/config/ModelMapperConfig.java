package mordvinov_dev.worker_service.mapping.config;

import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.worker_service.domain.document.Notification;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResult;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        log.info("MODEL_MAPPER_CONFIGURATION_START");

        final ModelMapper modelMapper = new ModelMapper();

        configureNotificationMappings(modelMapper);

        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true)
                .setFieldAccessLevel(
                        org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        log.info("MODEL_MAPPER_CONFIGURATION_SUCCESS");
        return modelMapper;
    }

    private void configureNotificationMappings(final ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<Notification, NotificationResult>() {
            @Override
            protected void configure() {
                map().setNotificationId(source.getId());
                map().setSuccess(source.getSent() != null && source.getSent());
                map().setMessage(source.getSent() != null && source.getSent()
                        ? "Notification sent successfully"
                        : "Notification not sent");
                map().setTimestamp(source.getSentAt() != null ? source.getSentAt() : source.getCreatedAt());
            }
        });
    }
}