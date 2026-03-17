package mordvinov_dev.api_gateway.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * Utility class for common filter operations in the API Gateway.
 * Provides methods for handling correlation IDs and request headers.
 */
@Component
public class FilterUtils {

    /**
     * Header name for correlation ID used in request tracing.
     */
    public static final String CORRELATION_ID = "X-Correlation-Id";
    
    /**
     * Header name for authorization token.
     */
    public static final String AUTH_TOKEN = "Authorization";

    /**
     * Extracts correlation ID from HTTP request headers.
     * 
     * @param requestHeaders the HTTP headers from the incoming request
     * @return the correlation ID if present, null otherwise
     */
    public String getCorrelationId(HttpHeaders requestHeaders) {
        if (requestHeaders.get(CORRELATION_ID) != null) {
            List<String> header = requestHeaders.get(CORRELATION_ID);
            return header.stream().findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Sets a custom header on the server web exchange request.
     * 
     * @param exchange the server web exchange to modify
     * @param name the header name to set
     * @param value the header value to set
     * @return the modified server web exchange with the new header
     */
    public ServerWebExchange setRequestHeader(ServerWebExchange exchange, String name, String value) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(name, value)
                        .build())
                .build();
    }

    /**
     * Sets the correlation ID header on the server web exchange request.
     * 
     * @param exchange the server web exchange to modify
     * @param correlationId the correlation ID to set in the header
     * @return the modified server web exchange with the correlation ID header
     */
    public ServerWebExchange setCorrelationId(ServerWebExchange exchange, String correlationId) {
        return this.setRequestHeader(exchange, CORRELATION_ID, correlationId);
    }

    /**
     * Generates a new unique correlation ID using UUID.
     * 
     * @return a newly generated correlation ID string
     */
    public String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
}