package mordvinov_dev.worker_service.exception;

public class NotificationSendException extends NotificationException {

    public NotificationSendException(String message, Throwable cause) {
        super(message, cause);
    }
}