package mordvinov_dev.billing_service.service.impl;

import mordvinov_dev.billing_service.config.YooKassaConfig;
import mordvinov_dev.billing_service.dto.request.CreatePaymentRequest;
import mordvinov_dev.billing_service.dto.request.CreateRefundRequest;
import mordvinov_dev.billing_service.dto.request.pageable.PageRequest;
import mordvinov_dev.billing_service.dto.response.PaymentResponse;
import mordvinov_dev.billing_service.dto.response.RefundResponse;
import mordvinov_dev.billing_service.dto.response.pageable.PageResponse;
import mordvinov_dev.billing_service.entity.PaymentEntity;
import mordvinov_dev.billing_service.entity.RefundEntity;
import mordvinov_dev.billing_service.exception.PaymentNotFoundException;
import mordvinov_dev.billing_service.exception.RefundNotFoundException;
import mordvinov_dev.billing_service.exception.YooKassaException;
import mordvinov_dev.billing_service.mapping.EntityMapper;
import mordvinov_dev.billing_service.repository.PaymentRepository;
import mordvinov_dev.billing_service.repository.RefundRepository;
import mordvinov_dev.billing_service.util.PaymentTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.loolzaaa.youkassa.client.ApiClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private YooKassaConfig config;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID userId;
    private UUID differentUserId;
    private String paymentId;
    private String refundId;
    private PaymentEntity paymentEntity;
    private PaymentEntity differentUserPaymentEntity;
    private RefundEntity refundEntity;
    private PaymentResponse paymentResponse;
    private RefundResponse refundResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        differentUserId = UUID.randomUUID();
        paymentId = PaymentTestDataFactory.TEST_PAYMENT_ID;
        refundId = PaymentTestDataFactory.TEST_REFUND_ID;

        paymentEntity = PaymentTestDataFactory.createPaymentEntity(userId);
        differentUserPaymentEntity = PaymentTestDataFactory.createPaymentEntityWithDifferentUser(differentUserId);
        refundEntity = PaymentTestDataFactory.createRefundEntity(userId);

        paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId(paymentId);
        paymentResponse.setUserId(userId);
        paymentResponse.setStatus("pending");

        refundResponse = new RefundResponse();
        refundResponse.setRefundId(refundId);
        refundResponse.setPaymentId(paymentId);
        refundResponse.setUserId(userId);
        refundResponse.setStatus("succeeded");
    }

    @Test
    void getPayment_Success() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.of(paymentEntity));
        when(entityMapper.map(paymentEntity, PaymentResponse.class)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.getPayment(paymentId, userId);

        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(userId, result.getUserId());
        verify(paymentRepository).findByPaymentIdAndUserId(paymentId, userId);
        verify(entityMapper).map(paymentEntity, PaymentResponse.class);
    }

    @Test
    void getPayment_ThrowsPaymentNotFoundException_WhenNotFound() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPayment(paymentId, userId);
        });

        verify(paymentRepository).findByPaymentIdAndUserId(paymentId, userId);
    }

    @Test
    void getPaymentsByUser_Success() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();
        Page<PaymentEntity> paymentsPage = new PageImpl<>(List.of(paymentEntity));

        when(paymentRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(paymentsPage);
        when(entityMapper.mapList(anyList(), eq(PaymentResponse.class)))
                .thenReturn(List.of(paymentResponse));

        PageResponse<PaymentResponse> result = paymentService.getPaymentsByUser(userId, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalElements());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        verify(paymentRepository).findAllByUserId(eq(userId), any(Pageable.class));
        verify(entityMapper).mapList(anyList(), eq(PaymentResponse.class));
    }

    @Test
    void getPaymentsByUser_ReturnsEmptyPage_WhenNoPayments() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();
        Page<PaymentEntity> emptyPage = new PageImpl<>(Collections.emptyList());

        when(paymentRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(entityMapper.mapList(anyList(), eq(PaymentResponse.class)))
                .thenReturn(Collections.emptyList());

        PageResponse<PaymentResponse> result = paymentService.getPaymentsByUser(userId, pageRequest);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(paymentRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void capturePayment_ThrowsPaymentNotFoundException_WhenNotFound() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.capturePayment(paymentId, userId);
        });

        verify(paymentRepository).findByPaymentIdAndUserId(paymentId, userId);
    }

    @Test
    void cancelPayment_ThrowsPaymentNotFoundException_WhenNotFound() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.cancelPayment(paymentId, userId);
        });

        verify(paymentRepository).findByPaymentIdAndUserId(paymentId, userId);
    }

    @Test
    void createRefund_ThrowsYooKassaException_WhenPaymentIdIsNull() {
        CreateRefundRequest request = PaymentTestDataFactory.createRefundRequestWithNullPaymentId();

        assertThrows(YooKassaException.class, () -> {
            paymentService.createRefund(request, userId);
        });
    }

    @Test
    void createRefund_ThrowsYooKassaException_WhenPaymentNotFound() {
        CreateRefundRequest request = PaymentTestDataFactory.createRefundRequest();
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.empty());

        assertThrows(YooKassaException.class, () -> {
            paymentService.createRefund(request, userId);
        });

        verify(paymentRepository).findByPaymentIdAndUserId(paymentId, userId);
    }

    @Test
    void getRefund_Success() {
        when(refundRepository.findByRefundIdAndUserId(refundId, userId))
                .thenReturn(Optional.of(refundEntity));
        when(entityMapper.map(refundEntity, RefundResponse.class)).thenReturn(refundResponse);

        RefundResponse result = paymentService.getRefund(refundId, userId);

        assertNotNull(result);
        assertEquals(refundId, result.getRefundId());
        assertEquals(paymentId, result.getPaymentId());
        verify(refundRepository).findByRefundIdAndUserId(refundId, userId);
        verify(entityMapper).map(refundEntity, RefundResponse.class);
    }

    @Test
    void getRefund_ThrowsRefundNotFoundException_WhenNotFound() {
        when(refundRepository.findByRefundIdAndUserId(refundId, userId))
                .thenReturn(Optional.empty());

        assertThrows(RefundNotFoundException.class, () -> {
            paymentService.getRefund(refundId, userId);
        });

        verify(refundRepository).findByRefundIdAndUserId(refundId, userId);
    }

    @Test
    void getRefundsByPayment_Success() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();
        Page<RefundEntity> refundsPage = new PageImpl<>(List.of(refundEntity));

        when(paymentRepository.existsByPaymentId(paymentId)).thenReturn(true);
        when(refundRepository.findAllByPaymentId(eq(paymentId), any(Pageable.class)))
                .thenReturn(refundsPage);
        when(entityMapper.mapList(anyList(), eq(RefundResponse.class)))
                .thenReturn(List.of(refundResponse));

        PageResponse<RefundResponse> result = paymentService.getRefundsByPayment(paymentId, userId, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(paymentRepository).existsByPaymentId(paymentId);
        verify(refundRepository).findAllByPaymentId(eq(paymentId), any(Pageable.class));
    }

    @Test
    void getRefundsByPayment_ReturnsEmptyPage_WhenNoRefunds() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();
        Page<RefundEntity> emptyPage = new PageImpl<>(Collections.emptyList());

        when(paymentRepository.existsByPaymentId(paymentId)).thenReturn(true);
        when(refundRepository.findAllByPaymentId(eq(paymentId), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(entityMapper.mapList(anyList(), eq(RefundResponse.class)))
                .thenReturn(Collections.emptyList());

        PageResponse<RefundResponse> result = paymentService.getRefundsByPayment(paymentId, userId, pageRequest);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(paymentRepository).existsByPaymentId(paymentId);
        verify(refundRepository).findAllByPaymentId(eq(paymentId), any(Pageable.class));
    }

    @Test
    void getRefundsByPayment_ThrowsPaymentNotFoundException_WhenPaymentNotFound() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();

        when(paymentRepository.existsByPaymentId(paymentId)).thenReturn(false);

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getRefundsByPayment(paymentId, userId, pageRequest);
        });

        verify(paymentRepository).existsByPaymentId(paymentId);
    }

    @Test
    void getRefundsByUser_Success() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();
        Page<RefundEntity> refundsPage = new PageImpl<>(List.of(refundEntity));

        when(refundRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(refundsPage);
        when(entityMapper.mapList(anyList(), eq(RefundResponse.class)))
                .thenReturn(List.of(refundResponse));

        PageResponse<RefundResponse> result = paymentService.getRefundsByUser(userId, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(refundRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getRefundsByUser_ReturnsEmptyPage_WhenNoRefunds() {
        PageRequest pageRequest = PaymentTestDataFactory.createPageRequest();
        Page<RefundEntity> emptyPage = new PageImpl<>(Collections.emptyList());

        when(refundRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(entityMapper.mapList(anyList(), eq(RefundResponse.class)))
                .thenReturn(Collections.emptyList());

        PageResponse<RefundResponse> result = paymentService.getRefundsByUser(userId, pageRequest);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(refundRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getPaymentByYooKassaId_ThrowsYooKassaException_WhenNotFound() {
        when(paymentRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.empty());

        assertThrows(YooKassaException.class, () -> {
            paymentService.getPaymentByYooKassaId(paymentId, userId);
        });

        verify(paymentRepository).findByPaymentId(paymentId);
    }

    @Test
    void getPaymentByYooKassaId_ThrowsYooKassaException_WhenUserNotAuthorized() {
        when(paymentRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.of(differentUserPaymentEntity));

        assertThrows(YooKassaException.class, () -> {
            paymentService.getPaymentByYooKassaId(paymentId, userId);
        });

        verify(paymentRepository).findByPaymentId(paymentId);
    }

    @Test
    void repositoryErrorHandling_PaymentRepositoryThrowsException() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> {
            paymentService.getPayment(paymentId, userId);
        });
    }

    @Test
    void repositoryErrorHandling_RefundRepositoryThrowsException() {
        when(refundRepository.findByRefundIdAndUserId(refundId, userId))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> {
            paymentService.getRefund(refundId, userId);
        });
    }

    @Test
    void mappingErrorHandling_EntityMapperThrowsException() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.of(paymentEntity));
        when(entityMapper.map(paymentEntity, PaymentResponse.class))
                .thenThrow(new RuntimeException("Mapping failed"));

        assertThrows(RuntimeException.class, () -> {
            paymentService.getPayment(paymentId, userId);
        });
    }

    @Test
    void configErrorHandling_ConfigReturnsNullValues() {
        when(config.getReturnUrl()).thenReturn(null);

        CreatePaymentRequest request = PaymentTestDataFactory.createPaymentRequest();
        assertThrows(YooKassaException.class, () -> {
            paymentService.createPayment(request, userId);
        });
    }

    @Test
    void paginationEdgeCase_LargePageRequest() {
        PageRequest largePageRequest = PaymentTestDataFactory.createLargePageRequest();
        Page<PaymentEntity> largePage = new PageImpl<>(
                List.of(paymentEntity),
                org.springframework.data.domain.PageRequest.of(100, 1000),
                50000L
        );

        when(paymentRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(largePage);
        when(entityMapper.mapList(anyList(), eq(PaymentResponse.class)))
                .thenReturn(List.of(paymentResponse));

        PageResponse<PaymentResponse> result = paymentService.getPaymentsByUser(userId, largePageRequest);

        assertNotNull(result);
        assertEquals(100, result.getCurrentPage());
        assertEquals(1000, result.getPageSize());
        verify(paymentRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "succeeded", "canceled", "waiting_for_capture"})
    void testPaymentStatusHandling(String status) {
        PaymentEntity paymentWithStatus = PaymentTestDataFactory.createPaymentEntityWithStatus(status);
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.of(paymentWithStatus));
        when(entityMapper.map(paymentWithStatus, PaymentResponse.class)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.getPayment(paymentId, userId);

        assertNotNull(result);
        verify(paymentRepository).findByPaymentIdAndUserId(paymentId, userId);
    }

    @Test
    void concurrentAccess_SamePaymentDifferentUsers() {
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.of(paymentEntity));
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, differentUserId))
                .thenReturn(Optional.empty());
        when(entityMapper.map(paymentEntity, PaymentResponse.class)).thenReturn(paymentResponse);

        PaymentResponse result1 = paymentService.getPayment(paymentId, userId);

        assertNotNull(result1);

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPayment(paymentId, differentUserId);
        });
    }

    @Test
    void createPayment_WithNullConfig() {
        when(config.getReturnUrl()).thenReturn(null);
        CreatePaymentRequest request = PaymentTestDataFactory.createPaymentRequest();

        assertThrows(YooKassaException.class, () -> {
            paymentService.createPayment(request, userId);
        });
    }

    @Test
    void createRefund_WithZeroAmount() {
        CreateRefundRequest request = PaymentTestDataFactory.createRefundRequestWithZeroAmount();
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.of(paymentEntity));

        assertThrows(YooKassaException.class, () -> {
            paymentService.createRefund(request, userId);
        });
    }

    @Test
    void createRefund_WithNegativeAmount() {
        CreateRefundRequest request = PaymentTestDataFactory.createRefundRequestWithNegativeAmount();
        when(paymentRepository.findByPaymentIdAndUserId(paymentId, userId))
                .thenReturn(Optional.of(paymentEntity));

        assertThrows(YooKassaException.class, () -> {
            paymentService.createRefund(request, userId);
        });
    }
}
