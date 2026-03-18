package mordvinov_dev.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mordvinov_dev.billing_service.config.TestSecurityConfig;
import mordvinov_dev.billing_service.config.TestWebhookServiceConfig;
import mordvinov_dev.billing_service.config.TestYooKassaConfig;
import mordvinov_dev.billing_service.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({TestYooKassaConfig.class, TestSecurityConfig.class, TestWebhookServiceConfig.class})
@EmbeddedKafka(
        partitions = 1,
        topics = {"payment.events"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
class WebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        doNothing().when(webhookService).processWebhook(any());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPayloadIsValid() throws Exception {
        String payload = """
                {
                    "event": "payment.succeeded",
                    "object": {
                        "id": "test-payment-id",
                        "status": "succeeded",
                        "amount": {
                            "value": "1000.00",
                            "currency": "RUB"
                        },
                        "metadata": {
                            "userEmail": "test@example.com"
                        }
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPayloadHasNoEvent() throws Exception {
        String payload = """
                {
                    "object": {
                        "id": "test-payment-id",
                        "status": "succeeded"
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPayloadHasNoObject() throws Exception {
        String payload = """
                {
                    "event": "payment.succeeded"
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPaymentCanceled() throws Exception {
        String payload = """
                {
                    "event": "payment.canceled",
                    "object": {
                        "id": "test-payment-id",
                        "status": "canceled",
                        "amount": {
                            "value": "1000.00",
                            "currency": "RUB"
                        }
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPaymentWaitingForCapture() throws Exception {
        String payload = """
                {
                    "event": "payment.waiting_for_capture",
                    "object": {
                        "id": "test-payment-id",
                        "status": "waiting_for_capture",
                        "amount": {
                            "value": "1000.00",
                            "currency": "RUB"
                        }
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenUnknownEvent() throws Exception {
        String payload = """
                {
                    "event": "payment.unknown",
                    "object": {
                        "id": "test-payment-id",
                        "status": "unknown",
                        "amount": {
                            "value": "1000.00",
                            "currency": "RUB"
                        }
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        String payload = """
                {
                    "event": "payment.succeeded",
                    "object": {
                        "id": "test-payment-id",
                        "status": "succeeded"
                    }
                }
                """;

        doThrow(new RuntimeException("Service error"))
                .when(webhookService).processWebhook(any());

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPayloadIsEmpty() throws Exception {
        String payload = "{}";

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPayloadHasComplexMetadata() throws Exception {
        String payload = """
                {
                    "event": "payment.succeeded",
                    "object": {
                        "id": "test-payment-id",
                        "status": "succeeded",
                        "amount": {
                            "value": "2000.00",
                            "currency": "RUB"
                        },
                        "metadata": {
                            "userEmail": "test@example.com",
                            "subscriptionId": "123e4567-e89b-12d3-a456-426614174000",
                            "description": "Premium subscription payment"
                        }
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void handleYooKassaWebhook_ShouldReturnOk_WhenPayloadHasMinimalData() throws Exception {
        String payload = """
                {
                    "event": "payment.succeeded",
                    "object": {
                        "id": "minimal-payment-id"
                    }
                }
                """;

        mockMvc.perform(post("/api/billing/webhook/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
