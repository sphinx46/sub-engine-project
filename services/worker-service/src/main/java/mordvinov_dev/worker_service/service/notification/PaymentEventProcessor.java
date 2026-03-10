package mordvinov_dev.worker_service.service.notification;

import mordvinov_dev.worker_service.event.PaymentEvent;

public interface PaymentEventProcessor {
    void process(PaymentEvent event);
}