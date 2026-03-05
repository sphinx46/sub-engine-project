package mordvinov_dev.billing_service.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface WebhookService {
    void processWebhook(JsonNode payload);
}