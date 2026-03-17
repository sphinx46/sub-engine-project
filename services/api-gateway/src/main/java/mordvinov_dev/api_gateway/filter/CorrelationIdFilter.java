package mordvinov_dev.api_gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that manages correlation IDs for request tracing.
 * Ensures every request has a correlation ID, either from the incoming request
 * headers or by generating a new one.
 * This filter runs with highest precedence to ensure correlation ID is available
 * for all subsequent filters and logging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    /**
     * Utility class for filter operations including correlation ID handling.
     */
    private final FilterUtils filterUtils;

    /**
     * Processes the incoming request by ensuring a correlation ID is present.
     * If no correlation ID exists in the request headers, a new one is generated
     * and added to the request.
     * 
     * @param exchange the server web exchange containing the request and response
     * @param chain the gateway filter chain to continue processing
     * @return a Mono that completes when the filter processing is done
     */
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

    /**
     * Returns the order precedence for this filter.
     * 
     * @return the filter order value (highest precedence)
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}