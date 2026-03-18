package mordvinov_dev.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Gateway configuration for rate limiting and key resolution.
 * Configures the primary key resolver for rate limiting based on authenticated
 * user principal or client IP address as fallback.
 */
@Configuration
public class GatewayConfig {

    /**
     * Creates the primary key resolver for rate limiting functionality.
     * Resolves rate limiting keys based on the authenticated user's principal name.
     * If no authenticated user is present, falls back to the client's IP address,
     * checking X-Forwarded-For header first, then the remote address.
     * 
     * @return a key resolver that uses user principal or IP address for rate limiting
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> principal.getName())
                .switchIfEmpty(Mono.defer(() -> {
                    String ip = Optional.ofNullable(
                                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                            .orElseGet(() ->
                                    Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                            .map(addr -> addr.getAddress().getHostAddress())
                                            .orElse("unknown"));
                    return Mono.just(ip);
                }));
    }
}