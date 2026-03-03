package mordvinov_dev.api_gateway.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Component
public class FilterUtils {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String AUTH_TOKEN = "Authorization";

    public String getCorrelationId(HttpHeaders requestHeaders) {
        if (requestHeaders.get(CORRELATION_ID) != null) {
            List<String> header = requestHeaders.get(CORRELATION_ID);
            return header.stream().findFirst().orElse(null);
        }
        return null;
    }

    public ServerWebExchange setRequestHeader(ServerWebExchange exchange, String name, String value) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(name, value)
                        .build())
                .build();
    }

    public ServerWebExchange setCorrelationId(ServerWebExchange exchange, String correlationId) {
        return this.setRequestHeader(exchange, CORRELATION_ID, correlationId);
    }

    public String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
}