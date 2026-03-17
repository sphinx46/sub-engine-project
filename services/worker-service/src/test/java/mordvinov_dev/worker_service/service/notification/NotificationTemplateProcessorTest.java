package mordvinov_dev.worker_service.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateProcessorTest {

    private NotificationTemplateProcessor templateProcessor;

    @BeforeEach
    void setUp() {
        templateProcessor = new NotificationTemplateProcessor();
    }

    @Test
    void process_paymentSuccessTemplate_returnsFormattedContent() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_123456789");
        data.put("amount", new BigDecimal("99.99"));
        data.put("currency", "USD");
        data.put("description", "Monthly subscription payment");

        String result = templateProcessor.process("payment-success", data);

        assertNotNull(result);
        assertTrue(result.contains("Уважаемый пользователь"));
        assertTrue(result.contains("Ваш платеж успешно выполнен"));
        assertTrue(result.contains("ID платежа: pay_123456789"));
        assertTrue(result.contains("Сумма: 99.99 USD"));
        assertTrue(result.contains("Описание: Monthly subscription payment"));
        assertTrue(result.contains("SubEngine Team"));
    }

    @Test
    void process_paymentSuccessTemplate_withMissingData_usesDefaults() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_987654321");

        String result = templateProcessor.process("payment-success", data);

        assertNotNull(result);
        assertTrue(result.contains("ID платежа: pay_987654321"));
        assertTrue(result.contains("Сумма: 0 RUB"));
        assertTrue(result.contains("Описание: —"));
    }

    @Test
    void process_paymentFailedTemplate_returnsFormattedContent() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_failed_123");
        data.put("amount", new BigDecimal("49.99"));
        data.put("currency", "EUR");
        data.put("description", "Payment processing failed");
        data.put("reason", "Insufficient funds");

        String result = templateProcessor.process("payment-failed", data);

        assertNotNull(result);
        assertTrue(result.contains("Уважаемый пользователь"));
        assertTrue(result.contains("К сожалению, ваш платеж не удался"));
        assertTrue(result.contains("ID платежа: pay_failed_123"));
        assertTrue(result.contains("Сумма: 49.99 EUR"));
        assertTrue(result.contains("Описание: Payment processing failed"));
        assertTrue(result.contains("Причина: Insufficient funds"));
        assertTrue(result.contains("Пожалуйста, попробуйте еще раз"));
        assertTrue(result.contains("SubEngine Team"));
    }

    @Test
    void process_paymentFailedTemplate_withMissingReason_usesDefaultReason() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_failed_456");
        data.put("amount", new BigDecimal("29.99"));
        data.put("currency", "GBP");
        data.put("description", "Failed payment");

        String result = templateProcessor.process("payment-failed", data);

        assertNotNull(result);
        assertTrue(result.contains("Причина: Неизвестная ошибка"));
    }

    @Test
    void process_paymentPendingTemplate_returnsFormattedContent() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_pending_123");
        data.put("amount", new BigDecimal("199.99"));
        data.put("currency", "JPY");
        data.put("description", "Payment waiting for capture");

        String result = templateProcessor.process("payment-pending", data);

        assertNotNull(result);
        assertTrue(result.contains("Уважаемый пользователь"));
        assertTrue(result.contains("Ваш платеж ожидает подтверждения"));
        assertTrue(result.contains("ID платежа: pay_pending_123"));
        assertTrue(result.contains("Сумма: 199.99 JPY"));
        assertTrue(result.contains("Описание: Payment waiting for capture"));
        assertTrue(result.contains("Мы уведомим вас о изменении статуса"));
        assertTrue(result.contains("SubEngine Team"));
    }

    @Test
    void process_paymentCanceledTemplate_returnsFormattedContent() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_canceled_123");
        data.put("amount", new BigDecimal("79.99"));
        data.put("currency", "USD");
        data.put("description", "Payment canceled by user");

        String result = templateProcessor.process("payment-canceled", data);

        assertNotNull(result);
        assertTrue(result.contains("Уважаемый пользователь"));
        assertTrue(result.contains("Ваш платеж был отменен"));
        assertTrue(result.contains("ID платежа: pay_canceled_123"));
        assertTrue(result.contains("Сумма: 79.99 USD"));
        assertTrue(result.contains("Описание: Payment canceled by user"));
        assertTrue(result.contains("Если вы не инициировали отмену"));
        assertTrue(result.contains("SubEngine Team"));
    }

    @Test
    void process_defaultTemplate_returnsFormattedContent() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_default_123");
        data.put("amount", new BigDecimal("150.00"));
        data.put("currency", "EUR");
        data.put("status", "unknown_status");
        data.put("description", "Payment with unknown status");

        String result = templateProcessor.process("unknown-template", data);

        assertNotNull(result);
        assertTrue(result.contains("Уважаемый пользователь"));
        assertTrue(result.contains("Статус вашего платежа изменился"));
        assertTrue(result.contains("ID платежа: pay_default_123"));
        assertTrue(result.contains("Сумма: 150.00 EUR"));
        assertTrue(result.contains("Статус: unknown_status"));
        assertTrue(result.contains("Описание: Payment with unknown status"));
        assertTrue(result.contains("SubEngine Team"));
    }

    @Test
    void process_defaultTemplate_withMissingStatus_usesDefaultStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_default_456");
        data.put("amount", new BigDecimal("25.00"));
        data.put("currency", "GBP");
        data.put("description", "Payment without status");

        String result = templateProcessor.process("unknown-template", data);

        assertNotNull(result);
        assertTrue(result.contains("Статус: UNKNOWN"));
    }

    @Test
    void process_emptyData_returnsTemplateWithDefaults() {
        Map<String, Object> data = new HashMap<>();

        String successResult = templateProcessor.process("payment-success", data);
        assertNotNull(successResult);
        assertTrue(successResult.contains("ID платежа: N/A"));
        assertTrue(successResult.contains("Сумма: 0 RUB"));
        assertTrue(successResult.contains("Описание: —"));

        String failedResult = templateProcessor.process("payment-failed", data);
        assertNotNull(failedResult);
        assertTrue(failedResult.contains("Причина: Неизвестная ошибка"));

        String defaultResult = templateProcessor.process("unknown", data);
        assertNotNull(defaultResult);
        assertTrue(defaultResult.contains("Статус: UNKNOWN"));
    }

    @Test
    void process_nullData_handlesGracefully() {
        assertThrows(NullPointerException.class, () -> {
            templateProcessor.process("payment-success", null);
        });
    }

    @Test
    void process_allTemplateTypes_returnCorrectContent() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_test_123");
        data.put("amount", new BigDecimal("100.00"));
        data.put("currency", "USD");
        data.put("description", "Test payment");
        data.put("reason", "Test reason");
        data.put("status", "test_status");

        String successResult = templateProcessor.process("payment-success", data);
        assertTrue(successResult.contains("Ваш платеж успешно выполнен"));

        String failedResult = templateProcessor.process("payment-failed", data);
        assertTrue(failedResult.contains("К сожалению, ваш платеж не удался"));
        assertTrue(failedResult.contains("Причина: Test reason"));

        String pendingResult = templateProcessor.process("payment-pending", data);
        assertTrue(pendingResult.contains("Ваш платеж ожидает подтверждения"));

        String canceledResult = templateProcessor.process("payment-canceled", data);
        assertTrue(canceledResult.contains("Ваш платеж был отменен"));

        String defaultResult = templateProcessor.process("non-existent", data);
        assertTrue(defaultResult.contains("Статус вашего платежа изменился"));
        assertTrue(defaultResult.contains("Статус: test_status"));
    }

    @Test
    void process_withSpecialCharacters_handlesCorrectly() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_спец_символы_123");
        data.put("amount", new BigDecimal("99.99"));
        data.put("currency", "RUB");
        data.put("description", "Платеж с русскими символами и \"кавычками\"");
        data.put("reason", "Ошибка: 'недостаточно средств'");

        String result = templateProcessor.process("payment-failed", data);

        assertNotNull(result);
        assertTrue(result.contains("ID платежа: pay_спец_символы_123"));
        assertTrue(result.contains("Описание: Платеж с русскими символами и \"кавычками\""));
        assertTrue(result.contains("Причина: Ошибка: 'недостаточно средств'"));
    }

    @Test
    void process_withNumericData_typesHandledCorrectly() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", 12345);
        data.put("amount", 99.99);
        data.put("currency", 840); // USD numeric code
        data.put("description", "Payment with numeric types");

        String result = templateProcessor.process("payment-success", data);

        assertNotNull(result);
        assertTrue(result.contains("ID платежа: 12345"));
        assertTrue(result.contains("Сумма: 99.99 840"));
        assertTrue(result.contains("Описание: Payment with numeric types"));
    }

    @Test
    void process_withUUIDData_handlesCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", "pay_uuid_test");
        data.put("amount", new BigDecimal("49.99"));
        data.put("currency", "EUR");
        data.put("userId", userId);
        data.put("subscriptionId", subscriptionId);
        data.put("description", "Payment with UUID data");

        String result = templateProcessor.process("default", data);

        assertNotNull(result);
        assertTrue(result.contains("ID платежа: pay_uuid_test"));
        assertTrue(result.contains("Описание: Payment with UUID data"));
        // UUID values should be converted to string representation, but they're not used in default template
        // The default template only uses: paymentId, amount, currency, status, description
    }
}
