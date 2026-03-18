package mordvinov_dev.subscription_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mordvinov_dev.subscription_service.config.TestBillingConfig;
import mordvinov_dev.subscription_service.dto.request.CreateSubscriptionRequest;
import mordvinov_dev.subscription_service.entity.Subscription;
import mordvinov_dev.subscription_service.entity.enums.PlanType;
import mordvinov_dev.subscription_service.entity.enums.StatusType;
import mordvinov_dev.subscription_service.repository.SubscriptionRepository;
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
@Import(TestBillingConfig.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {"premium-subscription-requests"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private UUID testUserId;
    private UUID testSubscriptionId;
    private String testUserEmail;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserEmail = "test@example.com";

        testSubscription = new Subscription();
        testSubscription.setUserId(testUserId);
        testSubscription.setPlanType(PlanType.FREE);
        testSubscription.setStatus(StatusType.ACTIVE);
        testSubscription.setCreatedAt(LocalDateTime.now());
        testSubscription.setUpdatedAt(LocalDateTime.now());
        testSubscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));

        testSubscription = subscriptionRepository.saveAndFlush(testSubscription);
        testSubscriptionId = testSubscription.getId();
    }

    @AfterEach
    void tearDown() {
        subscriptionRepository.deleteAll();
    }

    @Test
    void createSubscription_ShouldCreateFreeSubscription_WhenPlanTypeIsFree() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanType(PlanType.FREE);

        MvcResult result = mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Email", testUserEmail)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.planType").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextBillingDate").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        UUID createdSubscriptionId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        Subscription savedSubscription = subscriptionRepository.findById(createdSubscriptionId).orElse(null);
        assertThat(savedSubscription).isNotNull();
        assertThat(savedSubscription.getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(savedSubscription.getStatus()).isEqualTo(StatusType.ACTIVE);
        assertThat(savedSubscription.getNextBillingDate()).isNotNull();
    }

    @Test
    void createSubscription_ShouldCreatePremiumSubscription_WhenPlanTypeIsPremium() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanType(PlanType.PREMIUM);

        MvcResult result = mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Email", testUserEmail)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.planType").value("PREMIUM"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.confirmationUrl").value("https://test-payment-url.com/confirm/"))
                .andExpect(jsonPath("$.message").value("Subscription created. Please complete payment using the provided URL to activate."))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        UUID createdSubscriptionId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        Subscription savedSubscription = subscriptionRepository.findById(createdSubscriptionId).orElse(null);
        assertThat(savedSubscription).isNotNull();
        assertThat(savedSubscription.getPlanType()).isEqualTo(PlanType.PREMIUM);
        assertThat(savedSubscription.getStatus()).isEqualTo(StatusType.PENDING);
        assertThat(savedSubscription.getNextBillingDate()).isNull();
    }

    @Test
    void getUserSubscriptions_ShouldReturnUserSubscriptions_WhenUserExists() throws Exception {
        mockMvc.perform(get("/api/subscriptions/{userId}", testUserId)
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortedBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(testSubscriptionId.toString()))
                .andExpect(jsonPath("$.content[0].planType").value("FREE"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void getUserSubscriptions_ShouldReturnEmptyPage_WhenUserHasNoSubscriptions() throws Exception {
        UUID newUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/subscriptions/{userId}", newUserId)
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortedBy", "createdAt")
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
    void getUserSubscriptionsByStatus_ShouldReturnFilteredSubscriptions_WhenStatusProvided() throws Exception {
        Subscription pendingSubscription = new Subscription();
        pendingSubscription.setUserId(testUserId);
        pendingSubscription.setPlanType(PlanType.PREMIUM);
        pendingSubscription.setStatus(StatusType.PENDING);
        pendingSubscription.setCreatedAt(LocalDateTime.now().minusDays(1));
        pendingSubscription.setUpdatedAt(LocalDateTime.now().minusDays(1));

        pendingSubscription = subscriptionRepository.saveAndFlush(pendingSubscription);
        UUID anotherSubscriptionId = pendingSubscription.getId();

        mockMvc.perform(get("/api/subscriptions/{userId}/status/{status}", testUserId, StatusType.ACTIVE)
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortedBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(testSubscriptionId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        mockMvc.perform(get("/api/subscriptions/{userId}/status/{status}", testUserId, StatusType.PENDING)
                        .param("pageNumber", "0")
                        .param("size", "10")
                        .param("sortedBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(anotherSubscriptionId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void cancelSubscription_ShouldCancelSubscription_WhenSubscriptionIsActive() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/cancel", testSubscriptionId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.updatedAt").exists());

        Subscription cancelledSubscription = subscriptionRepository.findById(testSubscriptionId).orElse(null);
        assertThat(cancelledSubscription).isNotNull();
        assertThat(cancelledSubscription.getStatus()).isEqualTo(StatusType.CANCELLED);
    }

    @Test
    void cancelSubscription_ShouldReturnError_WhenSubscriptionAlreadyCancelled() throws Exception {
        subscriptionRepository.findById(testSubscriptionId).ifPresent(sub -> {
            sub.setStatus(StatusType.CANCELLED);
            subscriptionRepository.saveAndFlush(sub);
        });

        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/cancel", testSubscriptionId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    void getUserActiveSubscriptionsCount_ShouldReturnCorrectCount_WhenUserHasActiveSubscriptions() throws Exception {
        Subscription anotherSubscription = new Subscription();
        anotherSubscription.setUserId(testUserId);
        anotherSubscription.setPlanType(PlanType.PREMIUM);
        anotherSubscription.setStatus(StatusType.ACTIVE);
        anotherSubscription.setCreatedAt(LocalDateTime.now());
        anotherSubscription.setUpdatedAt(LocalDateTime.now());
        anotherSubscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        subscriptionRepository.saveAndFlush(anotherSubscription);

        Subscription cancelledSubscription = new Subscription();
        cancelledSubscription.setUserId(testUserId);
        cancelledSubscription.setPlanType(PlanType.FREE);
        cancelledSubscription.setStatus(StatusType.CANCELLED);
        cancelledSubscription.setCreatedAt(LocalDateTime.now());
        cancelledSubscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.saveAndFlush(cancelledSubscription);

        mockMvc.perform(get("/api/subscriptions/{userId}/active/count", testUserId))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void getUserActiveSubscriptionsCount_ShouldReturnZero_WhenUserHasNoActiveSubscriptions() throws Exception {
        subscriptionRepository.deleteAll();

        Subscription pendingSubscription = new Subscription();
        pendingSubscription.setUserId(testUserId);
        pendingSubscription.setPlanType(PlanType.PREMIUM);
        pendingSubscription.setStatus(StatusType.PENDING);
        pendingSubscription.setCreatedAt(LocalDateTime.now());
        subscriptionRepository.saveAndFlush(pendingSubscription);

        mockMvc.perform(get("/api/subscriptions/{userId}/active/count", testUserId))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void updateSubscriptionPlan_ShouldUpdateToPremium_WhenSubscriptionIsActive() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/plan", testSubscriptionId)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Email", testUserEmail)
                        .param("newPlan", "PREMIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSubscriptionId.toString()))
                .andExpect(jsonPath("$.planType").value("PREMIUM"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.confirmationUrl").value("https://test-payment-url.com/confirm/"))
                .andExpect(jsonPath("$.message").value("Plan updated to PREMIUM. Please complete payment using the provided URL to activate."));

        Subscription updatedSubscription = subscriptionRepository.findById(testSubscriptionId).orElse(null);
        assertThat(updatedSubscription).isNotNull();
        assertThat(updatedSubscription.getPlanType()).isEqualTo(PlanType.PREMIUM);
        assertThat(updatedSubscription.getStatus()).isEqualTo(StatusType.PENDING);
        assertThat(updatedSubscription.getNextBillingDate()).isNull();
    }

    @Test
    void updateSubscriptionPlan_ShouldUpdateToFree_WhenSubscriptionIsActive() throws Exception {
        subscriptionRepository.findById(testSubscriptionId).ifPresent(sub -> {
            sub.setPlanType(PlanType.PREMIUM);
            sub.setStatus(StatusType.ACTIVE);
            subscriptionRepository.saveAndFlush(sub);
        });

        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/plan", testSubscriptionId)
                        .header("X-User-Id", testUserId.toString())
                        .param("newPlan", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSubscriptionId.toString()))
                .andExpect(jsonPath("$.planType").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.confirmationUrl").doesNotExist());
    }

    @Test
    void getUserSubscriptions_withInvalidPagination_ShouldUseDefaultValues() throws Exception {
        mockMvc.perform(get("/api/subscriptions/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void endpoints_withNonExistentSubscription_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/cancel", nonExistentId)
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/plan", nonExistentId)
                        .header("X-User-Id", testUserId.toString())
                        .param("newPlan", "PREMIUM"))
                .andExpect(status().isNotFound());
    }
}