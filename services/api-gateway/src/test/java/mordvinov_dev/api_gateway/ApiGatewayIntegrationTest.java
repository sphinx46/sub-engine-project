package mordvinov_dev.api_gateway;

import mordvinov_dev.api_gateway.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Import(TestSecurityConfig.class)
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoads() {
        assertNotNull(webTestClient);
        assertNotNull(routeLocator);
    }

    @Test
    void gatewayRoutes_ShouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        
        assertNotNull(routes);
        assertTrue(routes.size() >= 1);
        
        assertTrue(routes.stream().anyMatch(route -> 
            route.getId().equals("test-route") &&
            route.getUri().toString().contains("httpbin.org")
        ));
    }

    @Test
    void actuatorInfo_ShouldBeAccessible() {
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void gateway_ShouldHandleGetRequests() {
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void gateway_ShouldHandleHeaders() {
        webTestClient.get()
                .uri("/actuator/info")
                .header("X-Custom-Header", "test-value")
                .exchange()
                .expectStatus().isOk();
    }
}
