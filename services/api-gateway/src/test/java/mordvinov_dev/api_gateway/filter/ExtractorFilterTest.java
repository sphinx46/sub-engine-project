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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractorFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private ExtractorFilter extractorFilter;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
    }

    @Test
    void filter_ShouldContinueWithoutHeaders_WhenNoAuthentication() {
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(extractorFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedencePlusOne() {
        int order = extractorFilter.getOrder();

        assertEquals(Integer.MIN_VALUE + 1, order);
    }

    @Test
    void filter_ShouldCompleteChain_WhenChainReturnsMono() {
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(extractorFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_ShouldPropagateError_WhenChainReturnsError() {
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("Test error")));

        StepVerifier.create(extractorFilter.filter(exchange, chain))
                .expectError(RuntimeException.class)
                .verify();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_ShouldCompleteWithOriginalExchange_WhenNoAuthentication() {
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(extractorFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        
        HttpHeaders headers = exchange.getRequest().getHeaders();
        assertEquals(null, headers.getFirst("X-User-Id"));
        assertEquals(null, headers.getFirst("X-User-Email"));
    }

    @Test
    void filter_ShouldHandleEmptySecurityContext() {
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(extractorFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_ShouldMaintainOriginalRequest_WhenNoAuthentication() {
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(extractorFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        
        assertEquals("/test", exchange.getRequest().getURI().getPath());
        assertEquals("GET", exchange.getRequest().getMethod().name());
    }
}
