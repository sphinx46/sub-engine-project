package mordvinov_dev.billing_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mordvinov_dev.billing_service.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling webhook callbacks from payment providers.
 * Processes incoming webhook notifications for payment status updates.
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Handles webhook callbacks from YooKassa payment system.
     * @param payload JSON payload from YooKassa
     * @return HTTP response indicating processing status
     */
    @PostMapping("/yookassa")
    public ResponseEntity<Void> handleYooKassaWebhook(@RequestBody JsonNode payload) {
        String eventType = payload.has("event") ? payload.get("event").asText() : "unknown";
        log.info("Received webhook from YooKassa: {}", eventType);

        try {
            webhookService.processWebhook(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}