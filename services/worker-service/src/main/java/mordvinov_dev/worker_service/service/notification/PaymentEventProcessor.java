package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.event.PaymentEvent;

/**
 * Interface for processing payment events and triggering appropriate actions.
 * Implementations should handle the business logic for different types of payment events.
 */
public interface PaymentEventProcessor {
    /**
     * Processes a payment event and executes the corresponding business logic.
     * 
     * @param event the payment event to process
     */
    void process(PaymentEvent event);
}