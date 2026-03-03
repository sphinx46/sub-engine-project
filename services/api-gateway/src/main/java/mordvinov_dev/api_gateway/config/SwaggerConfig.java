package mordvinov_dev.api_gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@Profile("!test")
public class SwaggerConfig {

    @Bean
    @Lazy(false)
    @ConditionalOnBean(RouteDefinitionLocator.class)
    public Set<SwaggerUrl> apis(RouteDefinitionLocator locator, SwaggerUiConfigProperties swaggerUiConfigProperties) {
        Set<SwaggerUrl> urls = new HashSet<>();

        List<org.springframework.cloud.gateway.route.RouteDefinition> definitions =
                locator.getRouteDefinitions().collectList().block();

        if (definitions != null) {
            definitions.stream()
                    .filter(routeDefinition -> routeDefinition.getId().matches(".*-service"))
                    .forEach(routeDefinition -> {
                        String name = routeDefinition.getId().replaceAll("-service", "");
                        SwaggerUrl swaggerUrl = new SwaggerUrl(
                                name,
                                "/v3/api-docs/" + name,
                                name + " Service"
                        );
                        urls.add(swaggerUrl);
                    });
        }

        swaggerUiConfigProperties.setUrls(urls);
        return urls;
    }
}