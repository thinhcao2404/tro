package com.example.payment_service.service;

import com.example.payment_service.config.VnPayConfig;
import com.example.payment_service.dto.event.PaymentCompletedEvent;
import com.example.payment_service.dto.response.VnPayIpnResponse;
import com.example.payment_service.entity.PaymentTransaction;
import com.example.payment_service.enums.PaymentMethod;
import com.example.payment_service.enums.PaymentStatus;
import com.example.payment_service.repository.PaymentTransactionRepository;
import com.example.payment_service.util.VnPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;
    @Mock
    private RestClient billingServiceRestClient;
    @Mock
    private RestClient contractServiceRestClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VnPayConfig vnPayConfig;
    private VnPayService vnPayService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        vnPayConfig = new VnPayConfig();
        vnPayConfig.setTmnCode("TESTTMN1");
        vnPayConfig.setHashSecret("TEST_SECRET_123456");
        vnPayConfig.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnPayConfig.setReturnUrl("http://localhost:8888/api/v1/payments/vnpay-return");
        vnPayService = new VnPayService(vnPayConfig);
        paymentService = new PaymentService(
                paymentRepository, billingServiceRestClient, contractServiceRestClient, vnPayService, eventPublisher);
    }

    @Test
    void handleVnPayIpn_validSuccess_updatesStatusAndPublishesEvent() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(1L)
                .transactionCode("PAY20261120001")
                .invoiceId(15L)
                .contractId(4L)
                .amount(new BigDecimal("3500000.00"))
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByTransactionCode("PAY20261120001")).thenReturn(Optional.of(tx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> params = signIpnParams(Map.of(
                "vnp_TxnRef", "PAY20261120001",
                "vnp_Amount", "350000000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00",
                "vnp_TransactionNo", "14556677"
        ));

        VnPayIpnResponse response = paymentService.handleVnPayIpn(params);

        assertEquals("00", response.getRspCode());
        assertEquals(PaymentStatus.SUCCESS, tx.getStatus());
        assertEquals("14556677", tx.getGatewayTransactionNo());
        assertNotNull(tx.getPaidAt());

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(15L, eventCaptor.getValue().getInvoiceId());
    }

    @Test
    void handleVnPayIpn_invalidSignature_returns97() {
        VnPayIpnResponse response = paymentService.handleVnPayIpn(Map.of(
                "vnp_TxnRef", "PAY20261120001",
                "vnp_SecureHash", "badhash"
        ));

        assertEquals("97", response.getRspCode());
        verifyNoInteractions(paymentRepository, eventPublisher);
    }

    @Test
    void handleVnPayIpn_amountMismatch_returns04() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionCode("PAY20261120001")
                .invoiceId(15L)
                .contractId(4L)
                .amount(new BigDecimal("3500000.00"))
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByTransactionCode("PAY20261120001")).thenReturn(Optional.of(tx));

        Map<String, String> params = signIpnParams(Map.of(
                "vnp_TxnRef", "PAY20261120001",
                "vnp_Amount", "10000000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00"
        ));

        VnPayIpnResponse response = paymentService.handleVnPayIpn(params);

        assertEquals("04", response.getRspCode());
        assertEquals(PaymentStatus.PENDING, tx.getStatus());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handleVnPayIpn_alreadyConfirmed_returns02() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionCode("PAY20261120001")
                .invoiceId(15L)
                .contractId(4L)
                .amount(new BigDecimal("3500000.00"))
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByTransactionCode("PAY20261120001")).thenReturn(Optional.of(tx));

        Map<String, String> params = signIpnParams(Map.of(
                "vnp_TxnRef", "PAY20261120001",
                "vnp_Amount", "350000000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00"
        ));

        VnPayIpnResponse response = paymentService.handleVnPayIpn(params);

        assertEquals("02", response.getRspCode());
        verifyNoInteractions(eventPublisher);
    }

    private Map<String, String> signIpnParams(Map<String, String> raw) {
        String query = VnPayUtil.buildSignedQuery(raw, vnPayConfig.getHashSecret());
        Map<String, String> result = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                result.put(
                        URLDecoder.decode(pair.substring(0, idx), StandardCharsets.US_ASCII),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.US_ASCII)
                );
            }
        }
        return result;
    }
}
