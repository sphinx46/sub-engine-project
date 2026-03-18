package mordvinov_dev.worker_service.service.notification.sender.email;

import mordvinov_dev.worker_service.domain.document.enums.NotificationChannel;
import mordvinov_dev.worker_service.domain.dto.request.NotificationRequest;
import mordvinov_dev.worker_service.domain.dto.response.NotificationResponse;
import mordvinov_dev.worker_service.service.notification.NotificationTemplateProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationTemplateProcessor templateProcessor;

    @InjectMocks
    private EmailNotificationSender emailNotificationSender;

    private NotificationRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test content")
                .build();
    }

    @Test
    void send_withDirectContent_sendsEmailSuccessfully() {
        NotificationResponse response = emailNotificationSender.send(testRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Email sent successfully", response.getMessage());
        assertNotNull(response.getNotificationId());
        assertNotNull(response.getTimestamp());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(templateProcessor, never()).process(anyString(), any());
    }

    @Test
    void send_withTemplate_sendsEmailWithProcessedContent() {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("paymentId", "pay_123456789");
        templateData.put("amount", new BigDecimal("99.99"));
        templateData.put("currency", "USD");
        templateData.put("description", "Test payment");

        NotificationRequest templateRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("template@example.com")
                .subject("Template Subject")
                .templateName("payment-success")
                .templateData(templateData)
                .build();

        String processedContent = "Processed template content";
        when(templateProcessor.process("payment-success", templateData)).thenReturn(processedContent);

        NotificationResponse response = emailNotificationSender.send(templateRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Email sent successfully", response.getMessage());

        verify(templateProcessor, times(1)).process("payment-success", templateData);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withMailException_returnsFailureResponse() {
        MailException mailException = new MailException("SMTP server unavailable") {};
        doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

        NotificationResponse response = emailNotificationSender.send(testRequest);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Failed to send email: SMTP server unavailable", response.getMessage());
        assertNotNull(response.getNotificationId());
        assertNotNull(response.getTimestamp());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withNullContentAndTemplate_returnsEmptyContent() {
        NotificationRequest emptyRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("empty@example.com")
                .subject("Empty Subject")
                .content(null)
                .templateName(null)
                .templateData(null)
                .build();

        NotificationResponse response = emailNotificationSender.send(emptyRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withTemplateButNullData_returnsEmptyContent() {
        NotificationRequest invalidTemplateRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("invalid@example.com")
                .subject("Invalid Template")
                .content(null)
                .templateName("payment-success")
                .templateData(null)
                .build();

        NotificationResponse response = emailNotificationSender.send(invalidTemplateRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(templateProcessor, never()).process(anyString(), any());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withTemplateNameButNullContent_usesTemplate() {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("paymentId", "pay_template_123");

        NotificationRequest templateRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("template@example.com")
                .subject("Template Subject")
                .content(null)
                .templateName("payment-failed")
                .templateData(templateData)
                .build();

        when(templateProcessor.process("payment-failed", templateData))
                .thenReturn("Failed payment template content");

        NotificationResponse response = emailNotificationSender.send(templateRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(templateProcessor, times(1)).process("payment-failed", templateData);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void getChannel_returnsEmailChannel() {
        NotificationChannel channel = emailNotificationSender.getChannel();

        assertEquals(NotificationChannel.EMAIL, channel);
    }

    @Test
    void send_verifiesMessageConstruction() {
        NotificationResponse response = emailNotificationSender.send(testRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withTemplateProcessorException_propagatesException() {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("paymentId", "pay_error_123");

        NotificationRequest templateRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("error@example.com")
                .subject("Error Subject")
                .content(null)
                .templateName("payment-success")
                .templateData(templateData)
                .build();

        when(templateProcessor.process("payment-success", templateData))
                .thenThrow(new RuntimeException("Template processing failed"));

        assertThrows(RuntimeException.class, () -> {
            emailNotificationSender.send(templateRequest);
        });

        verify(templateProcessor, times(1)).process("payment-success", templateData);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_generatesUniqueNotificationId() {
        NotificationResponse response1 = emailNotificationSender.send(testRequest);
        NotificationResponse response2 = emailNotificationSender.send(testRequest);

        assertNotNull(response1.getNotificationId());
        assertNotNull(response2.getNotificationId());
        assertNotEquals(response1.getNotificationId(), response2.getNotificationId());
    }

    @Test
    void send_withComplexTemplateData_sendsCorrectly() {
        Map<String, Object> complexTemplateData = new HashMap<>();
        complexTemplateData.put("paymentId", "pay_complex_123");
        complexTemplateData.put("amount", new BigDecimal("999.99"));
        complexTemplateData.put("currency", "EUR");
        complexTemplateData.put("description", "Complex payment description");
        complexTemplateData.put("userId", UUID.randomUUID());
        complexTemplateData.put("subscriptionId", UUID.randomUUID());
        complexTemplateData.put("reason", "Complex error reason");

        NotificationRequest complexRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("complex@example.com")
                .subject("Complex Template Subject")
                .templateName("payment-failed")
                .templateData(complexTemplateData)
                .build();

        String complexProcessedContent = "Complex processed template with all data";
        when(templateProcessor.process("payment-failed", complexTemplateData))
                .thenReturn(complexProcessedContent);

        NotificationResponse response = emailNotificationSender.send(complexRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(templateProcessor, times(1)).process("payment-failed", complexTemplateData);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withEmptyRecipient_stillAttemptsToSend() {
        NotificationRequest emptyRecipientRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("")
                .subject("Empty Recipient")
                .content("Content for empty recipient")
                .build();

        // This should still attempt to send, even though it might fail
        NotificationResponse response = emailNotificationSender.send(emptyRecipientRequest);

        assertNotNull(response);
        assertNotNull(response.getNotificationId());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_withSpecialCharactersInContent_handlesCorrectly() {
        String specialContent = "Content with русские символы and \"quotes\" and 'apostrophes' and \n newlines";
        
        NotificationRequest specialRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("special@example.com")
                .subject("Special Characters Subject: 中文")
                .content(specialContent)
                .build();

        NotificationResponse response = emailNotificationSender.send(specialRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void resolveContent_contentTakesPrecedenceOverTemplate() {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("paymentId", "pay_precedence_123");

        NotificationRequest precedenceRequest = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .channel(NotificationChannel.EMAIL)
                .recipient("precedence@example.com")
                .subject("Precedence Test")
                .content("Direct content should be used")
                .templateName("payment-success")
                .templateData(templateData)
                .build();

        NotificationResponse response = emailNotificationSender.send(precedenceRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(templateProcessor, never()).process(anyString(), any());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
