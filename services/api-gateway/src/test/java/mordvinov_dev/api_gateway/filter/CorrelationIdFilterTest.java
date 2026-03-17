package mordvinov_dev.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private FilterUtils filterUtils;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private CorrelationIdFilter correlationIdFilter;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
    }

    @Test
    void filter_ShouldSetNewCorrelationId_WhenNoCorrelationIdExists() {
        when(filterUtils.getCorrelationId(any(HttpHeaders.class))).thenReturn(null);
        when(filterUtils.generateCorrelationId()).thenReturn("generated-correlation-id");
        when(filterUtils.setCorrelationId(eq(exchange), eq("generated-correlation-id"))).thenReturn(exchange);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(correlationIdFilter.filter(exchange, chain))
                .verifyComplete();

        verify(filterUtils).getCorrelationId(exchange.getRequest().getHeaders());
        verify(filterUtils).generateCorrelationId();
        verify(filterUtils).setCorrelationId(exchange, "generated-correlation-id");
        verify(chain).filter(exchange);
    }

    @Test
    void filter_ShouldUseExistingCorrelationId_WhenCorrelationIdExists() {
        when(filterUtils.getCorrelationId(any(HttpHeaders.class))).thenReturn("existing-correlation-id");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(correlationIdFilter.filter(exchange, chain))
                .verifyComplete();

        verify(filterUtils).getCorrelationId(exchange.getRequest().getHeaders());
        verify(filterUtils, never()).generateCorrelationId();
        verify(filterUtils, never()).setCorrelationId(any(), any());
        verify(chain).filter(exchange);
    }

    @Test
    void filter_ShouldCompleteChain_WhenChainReturnsMono() {
        when(filterUtils.getCorrelationId(any(HttpHeaders.class))).thenReturn("test-correlation-id");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(correlationIdFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_ShouldPropagateError_WhenChainReturnsError() {
        when(filterUtils.getCorrelationId(any(HttpHeaders.class))).thenReturn("test-correlation-id");
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("Test error")));

        StepVerifier.create(correlationIdFilter.filter(exchange, chain))
                .expectError(RuntimeException.class)
                .verify();

        verify(chain).filter(exchange);
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedence() {
        int order = correlationIdFilter.getOrder();

        assertEquals(Integer.MIN_VALUE, order);
    }
}
