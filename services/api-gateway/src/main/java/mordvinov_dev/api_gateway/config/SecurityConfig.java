package mordvinov_dev.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.time.Duration;

/**
 * Security configuration for the API Gateway.
 * Configures OAuth2 JWT authentication and authorization rules for
 * protecting endpoints while allowing public access to certain paths.
 * Also configures JWT token validation with issuer and timestamp checks.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * OAuth2 issuer URI for JWT token validation.
     * Defaults to Keycloak server if not configured.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://keycloak:8080/realms/sub-engine}")
    private String issuerUri;

    /**
     * Configures the security filter chain for the API Gateway.
     * Sets up authentication requirements, public endpoints, and OAuth2 resource server configuration.
     * 
     * @param http the server HTTP security configuration builder
     * @return the configured security web filter chain
     */
    @Bean
    @Order(0)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring security with Issuer URI: {}", issuerUri);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/api/billing/webhook/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                )
                .build();
    }

    /**
     * Creates and configures a reactive JWT decoder for token validation.
     * Sets up issuer validation and timestamp validation with clock skew tolerance.
     * 
     * @return a configured reactive JWT decoder
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.info("Creating JWT decoder with Issuer URI: {}", issuerUri);

        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withClockSkew = new JwtTimestampValidator(Duration.ofSeconds(60));

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withClockSkew));

        return jwtDecoder;
    }
}