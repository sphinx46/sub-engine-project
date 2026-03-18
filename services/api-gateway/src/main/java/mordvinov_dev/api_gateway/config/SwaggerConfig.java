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

/**
 * Configuration class for Swagger/OpenAPI documentation in the API Gateway.
 * Automatically discovers and configures API documentation for all microservices
 * that follow the "-service" naming convention.
 * This configuration is active in all profiles except "test".
 */
@Configuration
@Profile("!test")
public class SwaggerConfig {

    /**
     * Creates a set of Swagger URLs for all discovered microservices.
     * Scans the gateway route definitions and automatically creates Swagger
     * documentation endpoints for services with names ending in "-service".
     * 
     * @param locator the route definition locator for discovering available services
     * @param swaggerUiConfigProperties the Swagger UI configuration properties to update
     * @return a set of Swagger URLs for all discovered microservices
     */
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