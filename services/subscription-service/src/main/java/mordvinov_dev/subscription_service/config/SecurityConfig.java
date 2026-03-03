package mordvinov_dev.subscription_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SECURITY: Configuring security filter chain with issuer: {}", issuerUri);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> {
                    log.debug("SECURITY: Configuring authorization rules");
                    authz
                            .requestMatchers(
                                    "/actuator/health",
                                    "/actuator/info",
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/api-docs/**",
                                    "/v3/api-docs/**"
                            ).permitAll()
                            .anyRequest().authenticated();
                    log.debug("SECURITY: Authorization rules configured");
                })
                .oauth2ResourceServer(oauth2 -> {
                    log.debug("SECURITY: Configuring OAuth2 Resource Server");
                    oauth2
                            .jwt(jwt -> {
                                log.debug("SECURITY: Configuring JWT decoder");
                                jwt.decoder(jwtDecoder());
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                            });
                });

        log.info("SECURITY: Security filter chain configured successfully");
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("JWT: Creating JWT decoder with issuer: {} and jwkSetUri: {}", issuerUri, jwkSetUri);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withClockSkew = new JwtTimestampValidator(Duration.ofSeconds(60));

        OAuth2TokenValidator<Jwt> loggingValidator = (Jwt token) -> {
            log.debug("JWT: Validating token - Subject: {}, Issuer: {}, Expires: {}",
                    token.getSubject(),
                    token.getIssuer(),
                    token.getExpiresAt());

            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
                log.error("JWT: Token expired at: {}", token.getExpiresAt());
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token expired", null)
                );
            }

            return OAuth2TokenValidatorResult.success();
        };

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                loggingValidator,
                withIssuer,
                withClockSkew
        ));

        log.info("JWT: JWT decoder created");
        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.debug("JWT: Creating JWT authentication converter");

        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        log.info("JWT: JWT authentication converter created");
        return jwtAuthenticationConverter;
    }
}