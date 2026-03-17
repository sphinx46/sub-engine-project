package mordvinov_dev.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mordvinov_dev.billing_service.config.TestSecurityConfig;
import mordvinov_dev.billing_service.config.TestYooKassaConfig;
import mordvinov_dev.billing_service.config.TestPaymentServiceConfig;
import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.entity.RefundEntity;
import mordvinov_dev.billing_service.repository.PaymentRepository;
import mordvinov_dev.billing_service.repository.RefundRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({TestYooKassaConfig.class, TestSecurityConfig.class, TestPaymentServiceConfig.class})
@EmbeddedKafka(
        partitions = 1,
        topics = {"payment.events"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    private UUID testUserId;
    private PaymentEntity testPayment;
    private RefundEntity testRefund;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testPayment = new PaymentEntity();
        testPayment.setPaymentId("test-payment-id-" + UUID.randomUUID());
        testPayment.setUserId(testUserId);
        testPayment.setSubscriptionId(UUID.randomUUID());
        testPayment.setStatus("pending");
        testPayment.setAmount(new BigDecimal("1000.00"));
        testPayment.setCurrency("RUB");
        testPayment.setDescription("Test payment");
        testPayment.setPaymentMethodId("test-method-id");
        testPayment.setPaymentMethodType("bank_card");
        testPayment.setUserEmail("test@example.com");
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        testPayment = paymentRepository.saveAndFlush(testPayment);

        testRefund = new RefundEntity();
        testRefund.setRefundId("test-refund-id-" + UUID.randomUUID());
        testRefund.setPaymentId(testPayment.getPaymentId());
        testRefund.setUserId(testUserId);
        testRefund.setStatus("succeeded");
        testRefund.setAmount(new BigDecimal("500.00"));
        testRefund.setCurrency("RUB");
        testRefund.setDescription("Test refund");
        testRefund.setCreatedAt(LocalDateTime.now());
        testRefund.setUpdatedAt(LocalDateTime.now());

        testRefund = refundRepository.saveAndFlush(testRefund);
    }

    @AfterEach
    void tearDown() {
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_ShouldCreatePayment_WhenRequestIsValid() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("2000.00"));
        request.setCurrency("RUB");
        request.setDescription("Test payment creation");
        request.setSubscriptionId(UUID.randomUUID());
        request.setCapture(true);
        request.setSavePaymentMethod(false);

        mockMvc.perform(post("/api/billing/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", testUserId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.paymentId").value("created-payment-id"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.amount").value(2000.00))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.description").value("Test payment creation"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getPayment_ShouldReturnPayment_WhenPaymentExists() throws Exception {
        mockMvc.perform(get("/api/billing/payments/{paymentId}", "test-payment-id")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.description").value("Test payment"))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void getUserPayments_ShouldReturnUserPayments_WhenUserExists() throws Exception {
        mockMvc.perform(get("/api/billing/payments")
                        .header("X-User-Id", testUserId.toString())
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.content[0].amount").value(1000.00))
                .andExpect(jsonPath("$.content[0].currency").value("RUB"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void capturePayment_ShouldCapturePayment_WhenPaymentExists() throws Exception {
        mockMvc.perform(post("/api/billing/payments/{paymentId}/capture", "test-payment-id")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.status").value("succeeded"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void cancelPayment_ShouldCancelPayment_WhenPaymentExists() throws Exception {
        mockMvc.perform(post("/api/billing/payments/{paymentId}/cancel", "test-payment-id")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.status").value("canceled"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getPaymentFromYooKassa_ShouldReturnPayment_WhenPaymentExists() throws Exception {
        mockMvc.perform(get("/api/billing/payments/yookassa/{paymentId}", "test-payment-id")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    void createRefund_ShouldCreateRefund_WhenRequestIsValid() throws Exception {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("test-payment-id");
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("RUB");
        request.setDescription("Partial refund");

        mockMvc.perform(post("/api/billing/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", testUserId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.refundId").value("created-refund-id"))
                .andExpect(jsonPath("$.paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.description").value("Partial refund"))
                .andExpect(jsonPath("$.status").value("succeeded"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getRefund_ShouldReturnRefund_WhenRefundExists() throws Exception {
        mockMvc.perform(get("/api/billing/refunds/{refundId}", "test-refund-id")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value("test-refund-id"))
                .andExpect(jsonPath("$.paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.description").value("Test refund"))
                .andExpect(jsonPath("$.status").value("succeeded"));
    }

    @Test
    void getRefundsByPayment_ShouldReturnRefunds_WhenPaymentExists() throws Exception {
        mockMvc.perform(get("/api/billing/payments/{paymentId}/refunds", "test-payment-id")
                        .header("X-User-Id", testUserId.toString())
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].refundId").value("test-refund-id"))
                .andExpect(jsonPath("$.content[0].paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.content[0].amount").value(500.00))
                .andExpect(jsonPath("$.content[0].currency").value("RUB"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void getUserRefunds_ShouldReturnUserRefunds_WhenUserExists() throws Exception {
        mockMvc.perform(get("/api/billing/refunds")
                        .header("X-User-Id", testUserId.toString())
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].refundId").value("test-refund-id"))
                .andExpect(jsonPath("$.content[0].paymentId").value("test-payment-id"))
                .andExpect(jsonPath("$.content[0].amount").value(500.00))
                .andExpect(jsonPath("$.content[0].currency").value("RUB"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void getUserPayments_withInvalidPagination_ShouldUseDefaultValues() throws Exception {
        mockMvc.perform(get("/api/billing/payments")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getUserRefunds_withInvalidPagination_ShouldUseDefaultValues() throws Exception {
        mockMvc.perform(get("/api/billing/refunds")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void endpoints_withNonExistentPayment_ShouldReturnNotFound() throws Exception {
        String nonExistentPaymentId = "non-existent-payment-id";

        mockMvc.perform(get("/api/billing/payments/{paymentId}", nonExistentPaymentId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/billing/payments/{paymentId}/capture", nonExistentPaymentId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/billing/payments/{paymentId}/cancel", nonExistentPaymentId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void endpoints_withNonExistentRefund_ShouldReturnNotFound() throws Exception {
        String nonExistentRefundId = "non-existent-refund-id";

        mockMvc.perform(get("/api/billing/refunds/{refundId}", nonExistentRefundId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getUserPayments_ShouldReturnEmptyPage_WhenUserHasNoPayments() throws Exception {
        UUID newUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/billing/payments")
                        .header("X-User-Id", newUserId.toString())
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getUserRefunds_ShouldReturnEmptyPage_WhenUserHasNoRefunds() throws Exception {
        UUID newUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/billing/refunds")
                        .header("X-User-Id", newUserId.toString())
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
}
