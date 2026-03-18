package mordvinov_dev.billing_service.config;

import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.dto.response.pageable.PageResponse;
import mordvinov_dev.billing_service.service.PaymentService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import mordvinov_dev.billing_service.exception.PaymentNotFoundException;
import mordvinov_dev.billing_service.exception.RefundNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@TestConfiguration
@Profile("test")
public class TestPaymentServiceConfig {

    @Bean
    @Primary
    public PaymentService mockPaymentService() {
        PaymentService mock = org.mockito.Mockito.mock(PaymentService.class);
        
        when(mock.createPayment(any(CreatePaymentRequest.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    CreatePaymentRequest request = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    PaymentResponse response = new PaymentResponse();
                    response.setId(UUID.randomUUID());
                    response.setPaymentId("created-payment-id");
                    response.setUserId(userId);
                    response.setAmount(request.getAmount());
                    response.setCurrency(request.getCurrency());
                    response.setDescription(request.getDescription());
                    response.setStatus("pending");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.getPayment(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    String paymentId = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    PaymentResponse response = new PaymentResponse();
                    response.setId(UUID.randomUUID());
                    response.setPaymentId(paymentId);
                    response.setUserId(userId);
                    response.setAmount(new BigDecimal("1000.00"));
                    response.setCurrency("RUB");
                    response.setDescription("Test payment");
                    response.setStatus("pending");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.getPaymentByYooKassaId(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    String paymentId = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    PaymentResponse response = new PaymentResponse();
                    response.setId(UUID.randomUUID());
                    response.setPaymentId(paymentId);
                    response.setUserId(userId);
                    response.setAmount(new BigDecimal("1000.00"));
                    response.setCurrency("RUB");
                    response.setDescription("Test payment");
                    response.setStatus("pending");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.capturePayment(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    String paymentId = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    PaymentResponse response = new PaymentResponse();
                    response.setId(UUID.randomUUID());
                    response.setPaymentId(paymentId);
                    response.setUserId(userId);
                    response.setAmount(new BigDecimal("1000.00"));
                    response.setCurrency("RUB");
                    response.setDescription("Test payment");
                    response.setStatus("succeeded");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.cancelPayment(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    String paymentId = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    PaymentResponse response = new PaymentResponse();
                    response.setId(UUID.randomUUID());
                    response.setPaymentId(paymentId);
                    response.setUserId(userId);
                    response.setAmount(new BigDecimal("1000.00"));
                    response.setCurrency("RUB");
                    response.setDescription("Test payment");
                    response.setStatus("canceled");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.getPaymentsByUser(any(UUID.class), any(PageRequest.class)))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    PageRequest pageRequest = invocation.getArgument(1);
                    
                    if (userId.toString().equals("00000000-0000-0000-0000-000000000001")) {
                        PageResponse<PaymentResponse> pageResponse = new PageResponse<>();
                        pageResponse.setContent(List.of());
                        pageResponse.setTotalElements(0L);
                        pageResponse.setTotalPages(0);
                        pageResponse.setCurrentPage(pageRequest.getPageNumber());
                        pageResponse.setPageSize(pageRequest.getSize());
                        pageResponse.setFirst(true);
                        pageResponse.setLast(true);
                        
                        return pageResponse;
                    } else if ((pageRequest.getSize() == 20 && pageRequest.getPageNumber() == 0) || 
                    (pageRequest.getSize() == 10 && pageRequest.getPageNumber() == 0)) {
                        PaymentResponse paymentResponse = new PaymentResponse();
                        paymentResponse.setId(UUID.randomUUID());
                        paymentResponse.setPaymentId("test-payment-id");
                        paymentResponse.setUserId(userId);
                        paymentResponse.setAmount(new BigDecimal("1000.00"));
                        paymentResponse.setCurrency("RUB");
                        paymentResponse.setDescription("Test payment");
                        paymentResponse.setStatus("pending");
                        paymentResponse.setCreatedAt(Instant.now());
                        paymentResponse.setUpdatedAt(Instant.now());
                        
                        PageResponse<PaymentResponse> pageResponse = new PageResponse<>();
                        pageResponse.setContent(List.of(paymentResponse));
                        pageResponse.setTotalElements(1L);
                        pageResponse.setTotalPages(1);
                        pageResponse.setCurrentPage(0);
                        pageResponse.setPageSize(pageRequest.getSize());
                        pageResponse.setFirst(true);
                        pageResponse.setLast(true);
                        
                        return pageResponse;
                    } else {
                        PageResponse<PaymentResponse> pageResponse = new PageResponse<>();
                        pageResponse.setContent(List.of());
                        pageResponse.setTotalElements(0L);
                        pageResponse.setTotalPages(0);
                        pageResponse.setCurrentPage(pageRequest.getPageNumber());
                        pageResponse.setPageSize(pageRequest.getSize());
                        pageResponse.setFirst(true);
                        pageResponse.setLast(true);
                        
                        return pageResponse;
                    }
                });
        
        when(mock.createRefund(any(CreateRefundRequest.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    CreateRefundRequest request = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    RefundResponse response = new RefundResponse();
                    response.setId(UUID.randomUUID());
                    response.setRefundId("created-refund-id");
                    response.setPaymentId(request.getPaymentId());
                    response.setUserId(userId);
                    response.setAmount(request.getAmount());
                    response.setCurrency(request.getCurrency());
                    response.setDescription(request.getDescription());
                    response.setStatus("succeeded");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.getRefund(any(String.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    String refundId = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    RefundResponse response = new RefundResponse();
                    response.setId(UUID.randomUUID());
                    response.setRefundId(refundId);
                    response.setPaymentId("test-payment-id");
                    response.setUserId(userId);
                    response.setAmount(new BigDecimal("500.00"));
                    response.setCurrency("RUB");
                    response.setDescription("Test refund");
                    response.setStatus("succeeded");
                    response.setCreatedAt(Instant.now());
                    response.setUpdatedAt(Instant.now());
                    return response;
                });
        
        when(mock.getRefundsByPayment(any(String.class), any(UUID.class), any(PageRequest.class)))
                .thenAnswer(invocation -> {
                    String paymentId = invocation.getArgument(0);
                    UUID userId = invocation.getArgument(1);
                    
                    RefundResponse refundResponse = new RefundResponse();
                    refundResponse.setId(UUID.randomUUID());
                    refundResponse.setRefundId("test-refund-id");
                    refundResponse.setPaymentId(paymentId);
                    refundResponse.setUserId(userId);
                    refundResponse.setAmount(new BigDecimal("500.00"));
                    refundResponse.setCurrency("RUB");
                    refundResponse.setDescription("Test refund");
                    refundResponse.setStatus("succeeded");
                    refundResponse.setCreatedAt(Instant.now());
                    refundResponse.setUpdatedAt(Instant.now());
                    
                    PageResponse<RefundResponse> pageResponse = new PageResponse<>();
                    pageResponse.setContent(List.of(refundResponse));
                    pageResponse.setTotalElements(1L);
                    pageResponse.setTotalPages(1);
                    pageResponse.setCurrentPage(0);
                    pageResponse.setPageSize(10);
                    pageResponse.setFirst(true);
                    pageResponse.setLast(true);
                    
                    return pageResponse;
                });
        
        when(mock.getRefundsByUser(any(UUID.class), any(PageRequest.class)))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    PageRequest pageRequest = invocation.getArgument(1);
                    
                    if (userId.toString().equals("00000000-0000-0000-0000-000000000001")) {
                        PageResponse<RefundResponse> pageResponse = new PageResponse<>();
                        pageResponse.setContent(List.of());
                        pageResponse.setTotalElements(0L);
                        pageResponse.setTotalPages(0);
                        pageResponse.setCurrentPage(pageRequest.getPageNumber());
                        pageResponse.setPageSize(pageRequest.getSize());
                        pageResponse.setFirst(true);
                        pageResponse.setLast(true);
                        
                        return pageResponse;
                    } else if ((pageRequest.getSize() == 20 && pageRequest.getPageNumber() == 0) || 
                    (pageRequest.getSize() == 10 && pageRequest.getPageNumber() == 0)) {
                        RefundResponse refundResponse = new RefundResponse();
                        refundResponse.setId(UUID.randomUUID());
                        refundResponse.setRefundId("test-refund-id");
                        refundResponse.setPaymentId("test-payment-id");
                        refundResponse.setUserId(userId);
                        refundResponse.setAmount(new BigDecimal("500.00"));
                        refundResponse.setCurrency("RUB");
                        refundResponse.setDescription("Test refund");
                        refundResponse.setStatus("succeeded");
                        refundResponse.setCreatedAt(Instant.now());
                        refundResponse.setUpdatedAt(Instant.now());
                        
                        PageResponse<RefundResponse> pageResponse = new PageResponse<>();
                        pageResponse.setContent(List.of(refundResponse));
                        pageResponse.setTotalElements(1L);
                        pageResponse.setTotalPages(1);
                        pageResponse.setCurrentPage(0);
                        pageResponse.setPageSize(pageRequest.getSize());
                        pageResponse.setFirst(true);
                        pageResponse.setLast(true);
                        
                        return pageResponse;
                    } else {
                        PageResponse<RefundResponse> pageResponse = new PageResponse<>();
                        pageResponse.setContent(List.of());
                        pageResponse.setTotalElements(0L);
                        pageResponse.setTotalPages(0);
                        pageResponse.setCurrentPage(pageRequest.getPageNumber());
                        pageResponse.setPageSize(pageRequest.getSize());
                        pageResponse.setFirst(true);
                        pageResponse.setLast(true);
                        
                        return pageResponse;
                    }
                });
        
        return mock;
    }
}
