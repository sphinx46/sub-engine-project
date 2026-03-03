package mordvinov_dev.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class GatewayConfig {

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