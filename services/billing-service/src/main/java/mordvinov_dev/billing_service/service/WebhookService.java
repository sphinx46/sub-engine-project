package mordvinov_dev.billing_service.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for processing webhooks from payment systems.
 * Handles incoming notifications about payment statuses and other events.
 */
public interface WebhookService {
    /**
     * Processes an incoming webhook from the payment system.
     * @param payload JSON webhook payload
     */
    void processWebhook(JsonNode payload);
}