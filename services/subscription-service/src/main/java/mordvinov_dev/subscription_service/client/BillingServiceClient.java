package mordvinov_dev.subscription_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.gateway.url:http://api-gateway:8084}")
    private String gatewayUrl;

    public String createPayment(UUID subscriptionId, UUID userId) {
        log.info("Calling billing service through gateway to create payment for subscription: {}, user: {}", subscriptionId, userId);
        log.info("Using gateway URL: {}", gatewayUrl);

        try {
            String url = UriComponentsBuilder.fromHttpUrl(gatewayUrl)
                    .path("/api/billing/payments")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            headers.set("Content-Type", "application/json");

            String jwtToken = extractJwtToken();
            if (jwtToken != null) {
                headers.set("Authorization", "Bearer " + jwtToken);
                log.debug("Added JWT token to request");
            } else {
                log.warn("No JWT token found in SecurityContext");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("subscriptionId", subscriptionId);
            requestBody.put("amount", 1000.00);
            requestBody.put("currency", "RUB");
            requestBody.put("description", "Premium subscription payment");
            requestBody.put("capture", true);
            requestBody.put("savePaymentMethod", false);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", userId.toString());
            metadata.put("subscriptionId", subscriptionId.toString());
            metadata.put("source", "subscription-service");
            requestBody.put("metadata", metadata);

            log.info("Sending request to: {}", url);
            log.debug("Request headers: {}", headers);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("confirmationUrl")) {
                String confirmationUrl = (String) response.getBody().get("confirmationUrl");
                log.info("Successfully created payment for subscription: {}, confirmationUrl: {}", subscriptionId, confirmationUrl);
                return confirmationUrl;
            } else {
                log.error("Billing service response missing confirmationUrl for subscription: {}. Response: {}",
                        subscriptionId, response.getBody());
                throw new RuntimeException("Failed to get confirmation URL from billing service");
            }

        } catch (Exception e) {
            log.error("Error calling billing service through gateway for subscription: {}, error: {}",
                    subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to create payment via gateway: " + e.getMessage(), e);
        }
    }

    private String extractJwtToken() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                return jwt.getTokenValue();
            }
        } catch (Exception e) {
            log.warn("Failed to extract JWT token: {}", e.getMessage());
        }
        return null;
    }
}