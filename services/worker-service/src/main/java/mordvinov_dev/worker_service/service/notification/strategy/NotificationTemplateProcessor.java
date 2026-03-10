package mordvinov_dev.worker_service.service.notification.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class NotificationTemplateProcessor {
    public String process(String templateName, Map<String, Object> data) {
        log.debug("Processing email template: {}", templateName);

        switch (templateName) {
            case "payment-success":
                return processPaymentSuccessTemplate(data);
            case "payment-failed":
                return processPaymentFailedTemplate(data);
            case "payment-pending":
                return processPaymentPendingTemplate(data);
            case "payment-canceled":
                return processPaymentCanceledTemplate(data);
            default:
                log.warn("Unknown template: {}, using default", templateName);
                return processDefaultTemplate(data);
        }
    }

    private String processPaymentSuccessTemplate(Map<String, Object> data) {
        return String.format("""
            Уважаемый пользователь,
            
            Ваш платеж успешно выполнен.
            
            Детали платежа:
            • ID платежа: %s
            • Сумма: %s %s
            • Описание: %s
            
            С уважением,
            SubEngine Team
            """,
                getValue(data, "paymentId", "N/A"),
                getValue(data, "amount", "0"),
                getValue(data, "currency", "RUB"),
                getValue(data, "description", "—")
        );
    }

    private String processPaymentFailedTemplate(Map<String, Object> data) {
        return String.format("""
            Уважаемый пользователь,
            
            К сожалению, ваш платеж не удался.
            
            Детали платежа:
            • ID платежа: %s
            • Сумма: %s %s
            • Описание: %s
            • Причина: %s
            
            Пожалуйста, попробуйте еще раз или обратитесь в поддержку.
            
            С уважением,
            SubEngine Team
            """,
                getValue(data, "paymentId", "N/A"),
                getValue(data, "amount", "0"),
                getValue(data, "currency", "RUB"),
                getValue(data, "description", "—"),
                getValue(data, "reason", "Неизвестная ошибка")
        );
    }

    private String processPaymentPendingTemplate(Map<String, Object> data) {
        return String.format("""
            Уважаемый пользователь,
            
            Ваш платеж ожидает подтверждения.
            
            Детали платежа:
            • ID платежа: %s
            • Сумма: %s %s
            • Описание: %s
            
            Мы уведомим вас о изменении статуса.
            
            С уважением,
            SubEngine Team
            """,
                getValue(data, "paymentId", "N/A"),
                getValue(data, "amount", "0"),
                getValue(data, "currency", "RUB"),
                getValue(data, "description", "—")
        );
    }

    private String processPaymentCanceledTemplate(Map<String, Object> data) {
        return String.format("""
            Уважаемый пользователь,
            
            Ваш платеж был отменен.
            
            Детали платежа:
            • ID платежа: %s
            • Сумма: %s %s
            • Описание: %s
            
            Если вы не инициировали отмену, пожалуйста, обратитесь в поддержку.
            
            С уважением,
            SubEngine Team
            """,
                getValue(data, "paymentId", "N/A"),
                getValue(data, "amount", "0"),
                getValue(data, "currency", "RUB"),
                getValue(data, "description", "—")
        );
    }

    private String processDefaultTemplate(Map<String, Object> data) {
        return String.format("""
            Уважаемый пользователь,
            
            Статус вашего платежа изменился.
            
            Детали платежа:
            • ID платежа: %s
            • Сумма: %s %s
            • Статус: %s
            • Описание: %s
            
            С уважением,
            SubEngine Team
            """,
                getValue(data, "paymentId", "N/A"),
                getValue(data, "amount", "0"),
                getValue(data, "currency", "RUB"),
                getValue(data, "status", "UNKNOWN"),
                getValue(data, "description", "—")
        );
    }

    private Object getValue(Map<String, Object> data, String key, Object defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }
}
