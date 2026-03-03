package mordvinov_dev.api_gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private final FilterUtils filterUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = filterUtils.getCorrelationId(exchange.getRequest().getHeaders());

        if (correlationId == null) {
            correlationId = filterUtils.generateCorrelationId();
            exchange = filterUtils.setCorrelationId(exchange, correlationId);
        }

        log.debug("Correlation ID: {}", correlationId);

        String finalCorrelationId = correlationId;
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() ->
                        log.debug("Completed request with Correlation ID: {}", finalCorrelationId)
                ));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}