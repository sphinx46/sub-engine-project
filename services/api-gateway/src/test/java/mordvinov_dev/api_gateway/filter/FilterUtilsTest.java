package mordvinov_dev.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FilterUtilsTest {

    private FilterUtils filterUtils;

    @BeforeEach
    void setUp() {
        filterUtils = new FilterUtils();
    }

    @Test
    void getCorrelationId_ShouldReturnCorrelationId_WhenHeaderExists() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(FilterUtils.CORRELATION_ID, List.of("test-correlation-id"));

        String result = filterUtils.getCorrelationId(headers);

        assertEquals("test-correlation-id", result);
    }

    @Test
    void getCorrelationId_ShouldReturnNull_WhenHeaderDoesNotExist() {
        HttpHeaders headers = new HttpHeaders();

        String result = filterUtils.getCorrelationId(headers);

        assertNull(result);
    }

    @Test
    void getCorrelationId_ShouldReturnNull_WhenHeaderIsEmpty() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(FilterUtils.CORRELATION_ID, List.of());

        String result = filterUtils.getCorrelationId(headers);

        assertNull(result);
    }

    @Test
    void getCorrelationId_ShouldReturnFirstValue_WhenHeaderHasMultipleValues() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(FilterUtils.CORRELATION_ID, List.of("first-id", "second-id"));

        String result = filterUtils.getCorrelationId(headers);

        assertEquals("first-id", result);
    }

    @Test
    void setRequestHeader_ShouldAddHeader_WhenCalled() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerWebExchange result = filterUtils.setRequestHeader(exchange, "X-Test-Header", "test-value");

        assertNotNull(result);
        assertEquals("test-value", result.getRequest().getHeaders().getFirst("X-Test-Header"));
    }

    @Test
    void setCorrelationId_ShouldAddCorrelationHeader_WhenCalled() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerWebExchange result = filterUtils.setCorrelationId(exchange, "test-correlation-id");

        assertNotNull(result);
        assertEquals("test-correlation-id", result.getRequest().getHeaders().getFirst(FilterUtils.CORRELATION_ID));
    }

    @Test
    void generateCorrelationId_ShouldReturnUUID_WhenCalled() {
        String result = filterUtils.generateCorrelationId();

        assertNotNull(result);
        assertTrue(result.matches("[a-f0-9-]{36}"));
        
        String secondResult = filterUtils.generateCorrelationId();
        assertNotEquals(result, secondResult);
    }

    @Test
    void constants_ShouldHaveCorrectValues() {
        assertEquals("X-Correlation-Id", FilterUtils.CORRELATION_ID);
        assertEquals("Authorization", FilterUtils.AUTH_TOKEN);
    }
}
