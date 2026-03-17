package mordvinov_dev.api_gateway.filter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that extracts user information from JWT authentication tokens
 * and adds it as HTTP headers for downstream services.
 * This filter runs with highest precedence + 1 to ensure it executes after
 * correlation ID filtering but before other filters.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ExtractorFilter implements GlobalFilter, Ordered {

    /**
     * Header name for user ID extracted from JWT.
     */
    private static final String USER_ID_HEADER = "X-User-Id";
    
    /**
     * Header name for user email extracted from JWT.
     */
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    
    /**
     * JWT claim name for user ID (subject).
     */
    private static final String USER_ID_CLAIM = "sub";
    
    /**
     * JWT claim name for user email.
     */
    private static final String USER_EMAIL_CLAIM = "email";

    /**
     * Initializes the filter and logs its execution order.
     * This method is called after dependency injection is complete.
     */
    @PostConstruct
    public void init() {
        log.info("UserIdExtractorFilter initialized with order: {}", getOrder());
    }

    /**
     * Processes the incoming request by extracting user information from JWT token
     * and adding it as headers to the request for downstream services.
     * 
     * @param exchange the server web exchange containing the request and response
     * @param chain the gateway filter chain to continue processing
     * @return a Mono that completes when the filter processing is done
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        log.debug("Processing request: {} {}", request.getMethod(), request.getURI().getPath());

        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {
                    String userId = jwt.getClaimAsString(USER_ID_CLAIM);
                    String userEmail = jwt.getClaimAsString(USER_EMAIL_CLAIM);

                    if (userId != null) {
                        log.debug("Extracted userId: {}, email: {} for path: {}", userId, userEmail, request.getURI().getPath());

                        ServerHttpRequest.Builder requestBuilder = request.mutate()
                                .header(USER_ID_HEADER, userId);

                        if (userEmail != null && !userEmail.isEmpty()) {
                            requestBuilder.header(USER_EMAIL_HEADER, userEmail);
                            log.debug("Added email header: {} for user: {}", userEmail, userId);
                        }

                        ServerHttpRequest mutatedRequest = requestBuilder.build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }

                    log.warn("userId claim not found in JWT for path: {}", request.getURI().getPath());
                    return chain.filter(exchange);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("No authenticated user found for path: {}", request.getURI().getPath());
                    return chain.filter(exchange);
                }));
    }

    /**
     * Returns the order precedence for this filter.
     * 
     * @return the filter order value (highest precedence + 1)
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}