package com.example.payment_service.service;

import com.example.payment_service.dto.event.PaymentCompletedEvent;
import com.example.payment_service.dto.request.PaymentConfirmRequest;
import com.example.payment_service.dto.request.PaymentCreateRequest;
import com.example.payment_service.dto.response.*;
import com.example.payment_service.entity.PaymentTransaction;
import com.example.payment_service.enums.PaymentMethod;
import com.example.payment_service.enums.PaymentStatus;
import com.example.payment_service.exception.BadRequestException;
import com.example.payment_service.exception.ExternalServiceException;
import com.example.payment_service.exception.ResourceNotFoundException;
import com.example.payment_service.exception.ServiceUnavailableException;
import com.example.payment_service.repository.PaymentTransactionRepository;
import com.example.payment_service.util.PageResponseUtil;
import com.example.payment_service.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final DateTimeFormatter TX_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentTransactionRepository paymentRepository;
    private final RestClient billingServiceRestClient;
    private final RestClient contractServiceRestClient;
    private final VnPayService vnPayService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request, Jwt jwt, HttpServletRequest httpRequest) {
        boolean isLandlord = hasRole(jwt, "LANDLORD");
        if (request.getMethod() == PaymentMethod.CASH && !isLandlord) {
            throw new AccessDeniedException("Chỉ chủ trọ mới được ghi nhận thanh toán tiền mặt.");
        }

        InvoiceSummary invoice = fetchInvoice(request.getInvoiceId());
        if (!"UNPAID".equalsIgnoreCase(invoice.status())) {
            throw new BadRequestException("Hóa đơn không ở trạng thái UNPAID.");
        }
        if (invoice.totalAmount() == null || invoice.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền hóa đơn không hợp lệ để thanh toán.");
        }
        if (paymentRepository.existsByInvoiceIdAndStatus(invoice.id(), PaymentStatus.SUCCESS)) {
            throw new BadRequestException("Hóa đơn này đã có giao dịch thanh toán thành công.");
        }

        // Hủy các giao dịch PENDING cũ của cùng hóa đơn để tránh tồn tại nhiều mã chờ
        List<PaymentTransaction> oldPending = paymentRepository.findByInvoiceIdAndStatus(invoice.id(), PaymentStatus.PENDING);
        for (PaymentTransaction pending : oldPending) {
            pending.setStatus(PaymentStatus.CANCELLED);
        }
        if (!oldPending.isEmpty()) {
            paymentRepository.saveAll(oldPending);
        }

        Long tenantId = resolveTenantId(jwt, invoice.contractId());
        boolean isImmediateSuccess = request.getMethod() == PaymentMethod.CASH;
        LocalDateTime now = LocalDateTime.now();

        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionCode(generateTransactionCode())
                .invoiceId(invoice.id())
                .contractId(invoice.contractId())
                .tenantId(tenantId)
                .amount(invoice.totalAmount())
                .method(request.getMethod())
                .status(isImmediateSuccess ? PaymentStatus.SUCCESS : PaymentStatus.PENDING)
                .bankCode(request.getBankCode())
                .note(request.getNote())
                .paidAt(isImmediateSuccess ? now : null)
                .build();

        String paymentUrl = null;
        if (request.getMethod() == PaymentMethod.VNPAY) {
            paymentUrl = vnPayService.buildPaymentUrl(tx, VnPayUtil.getClientIp(httpRequest));
        }

        PaymentTransaction saved = paymentRepository.save(tx);
        if (isImmediateSuccess) {
            publishCompletedEvent(saved);
        }

        return toResponse(saved, paymentUrl);
    }

    @Transactional
    public PaymentResponse confirmPayment(String transactionCode, PaymentConfirmRequest request) {
        PaymentTransaction tx = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionCode));

        if (tx.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể xác nhận giao dịch đang ở trạng thái PENDING.");
        }
        if (paymentRepository.existsByInvoiceIdAndStatus(tx.getInvoiceId(), PaymentStatus.SUCCESS)) {
            throw new BadRequestException("Hóa đơn này đã được thanh toán thành công trước đó.");
        }

        tx.setStatus(PaymentStatus.SUCCESS);
        tx.setPaidAt(LocalDateTime.now());
        if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
            tx.setNote(request.getNote().trim());
        }

        PaymentTransaction saved = paymentRepository.save(tx);
        publishCompletedEvent(saved);
        return toResponse(saved, null);
    }

    @Transactional
    public PaymentResponse cancelPayment(String transactionCode, Jwt jwt) {
        PaymentTransaction tx = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionCode));

        if (!hasRole(jwt, "LANDLORD")) {
            Long tenantId = extractTenantId(jwt);
            if (tenantId == null || !tenantId.equals(tx.getTenantId())) {
                throw new AccessDeniedException("Bạn không có quyền hủy giao dịch này.");
            }
        }
        if (tx.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy giao dịch đang ở trạng thái PENDING.");
        }

        tx.setStatus(PaymentStatus.CANCELLED);
        return toResponse(paymentRepository.save(tx), null);
    }

    @Transactional
    public VnPayIpnResponse handleVnPayIpn(Map<String, String> params) {
        try {
            if (!vnPayService.verifySignature(params)) {
                return VnPayIpnResponse.of("97", "Invalid Signature");
            }

            String txnRef = params.get("vnp_TxnRef");
            if (txnRef == null || txnRef.isBlank()) {
                return VnPayIpnResponse.of("01", "Order not Found");
            }

            PaymentTransaction tx = paymentRepository.findByTransactionCode(txnRef).orElse(null);
            if (tx == null) {
                return VnPayIpnResponse.of("01", "Order not Found");
            }

            BigDecimal vnpAmount;
            try {
                vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
            } catch (Exception e) {
                return VnPayIpnResponse.of("04", "Invalid Amount");
            }

            if (vnpAmount.compareTo(tx.getAmount()) != 0) {
                return VnPayIpnResponse.of("04", "Invalid Amount");
            }

            if (tx.getStatus() != PaymentStatus.PENDING) {
                return VnPayIpnResponse.of("02", "Order already confirmed");
            }

            tx.setGatewayTransactionNo(params.get("vnp_TransactionNo"));
            boolean paidSuccess = "00".equals(params.get("vnp_ResponseCode"))
                    && "00".equals(params.get("vnp_TransactionStatus"));

            if (paidSuccess) {
                tx.setStatus(PaymentStatus.SUCCESS);
                tx.setPaidAt(LocalDateTime.now());
                PaymentTransaction saved = paymentRepository.save(tx);
                publishCompletedEvent(saved);
            } else {
                tx.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(tx);
            }

            return VnPayIpnResponse.of("00", "Confirm Success");
        } catch (Exception ex) {
            log.error("Lỗi xử lý VNPay IPN: {}", ex.getMessage(), ex);
            return VnPayIpnResponse.of("99", "Unknown error");
        }
    }

    public VnPayReturnResponse handleVnPayReturn(Map<String, String> params) {
        boolean valid = vnPayService.verifySignature(params);
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = valid && "00".equals(responseCode) && "00".equals(transactionStatus);

        BigDecimal amount = null;
        try {
            if (params.get("vnp_Amount") != null) {
                amount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
            }
        } catch (NumberFormatException ignored) {
        }

        String message = !valid
                ? "Chữ ký VNPay không hợp lệ."
                : (success ? "Thanh toán VNPay thành công." : "Thanh toán VNPay không thành công (mã: " + responseCode + ").");

        return VnPayReturnResponse.builder()
                .validSignature(valid)
                .success(success)
                .transactionCode(params.get("vnp_TxnRef"))
                .amount(amount)
                .responseCode(responseCode)
                .transactionStatus(transactionStatus)
                .gatewayTransactionNo(params.get("vnp_TransactionNo"))
                .message(message)
                .build();
    }

    public PageResponse<PaymentResponse> getPayments(
            Long invoiceId,
            String status,
            String method,
            int page,
            int size,
            String sort,
            Jwt jwt) {

        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Page phải >= 0 và size phải trong khoảng 1..100.");
        }

        Specification<PaymentTransaction> spec = (root, query, cb) -> cb.conjunction();

        if (!hasRole(jwt, "LANDLORD")) {
            Long tenantId = extractTenantId(jwt);
            if (tenantId == null) {
                throw new BadRequestException("Token không chứa thông tin tenantId.");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId));
        }

        if (invoiceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("invoiceId"), invoiceId));
        }

        if (status != null && !status.isBlank()) {
            try {
                PaymentStatus enumStatus = PaymentStatus.valueOf(status.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), enumStatus));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái thanh toán '" + status + "' không hợp lệ.");
            }
        }

        if (method != null && !method.isBlank()) {
            try {
                PaymentMethod enumMethod = PaymentMethod.valueOf(method.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), enumMethod));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Phương thức thanh toán '" + method + "' không hợp lệ.");
            }
        }

        String[] sortParams = (sort == null || sort.isBlank()) ? new String[0] : sort.split(",");
        String rawField = sortParams.length > 0 ? sortParams[0].trim() : "id";
        String sortField = switch (rawField) {
            case "id", "amount", "status", "method", "paidAt", "createdAt" -> rawField;
            default -> "id";
        };
        Sort.Direction direction = (sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<PaymentResponse> responsePage = paymentRepository.findAll(spec, pageable)
                .map(tx -> toResponse(tx, null));
        return PageResponseUtil.from(responsePage);
    }

    public PaymentResponse getPaymentById(Long id, Jwt jwt) {
        PaymentTransaction tx = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch ID: " + id));

        if (!hasRole(jwt, "LANDLORD")) {
            Long tenantId = extractTenantId(jwt);
            if (tenantId == null || !tenantId.equals(tx.getTenantId())) {
                throw new AccessDeniedException("Bạn không có quyền xem giao dịch này.");
            }
        }
        return toResponse(tx, null);
    }

    private InvoiceSummary fetchInvoice(Long invoiceId) {
        try {
            InvoiceSummary invoice = billingServiceRestClient.get()
                    .uri("/api/v1/invoices/{id}", invoiceId)
                    .retrieve()
                    .body(InvoiceSummary.class);
            if (invoice == null) {
                throw new ResourceNotFoundException("Không tìm thấy hóa đơn ID: " + invoiceId);
            }
            return invoice;
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("billing-service hiện không khả dụng.");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Không tìm thấy hóa đơn ID: " + invoiceId);
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new AccessDeniedException("Bạn không có quyền thanh toán hóa đơn này.");
            }
            throw new ExternalServiceException("Lỗi từ billing-service: " + e.getStatusCode().value());
        }
    }

    private void publishCompletedEvent(PaymentTransaction tx) {
        eventPublisher.publishEvent(PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionCode(tx.getTransactionCode())
                .invoiceId(tx.getInvoiceId())
                .contractId(tx.getContractId())
                .amount(tx.getAmount())
                .method(tx.getMethod().name())
                .paidAt(tx.getPaidAt())
                .build());
    }

    private boolean hasRole(Jwt jwt, String expectedRole) {
        if (jwt == null) {
            return false;
        }
        String role = jwt.getClaimAsString("role");
        return expectedRole.equals(role) || ("ROLE_" + expectedRole).equals(role);
    }

    private Long resolveTenantId(Jwt jwt, Long contractId) {
        Long tenantId = extractTenantId(jwt);
        if (tenantId != null || contractId == null) {
            return tenantId;
        }
        try {
            ContractSummary contract = contractServiceRestClient.get()
                    .uri("/api/v1/contracts/{contractId}", contractId)
                    .retrieve()
                    .body(ContractSummary.class);
            return contract != null ? contract.tenantId() : null;
        } catch (Exception ex) {
            log.warn("Không lấy được tenantId từ contract-service cho contractId={}: {}", contractId, ex.getMessage());
            return null;
        }
    }

    private Long extractTenantId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object claim = jwt.getClaim("tenantId");
        return (claim instanceof Number n) ? n.longValue() : null;
    }

    private String generateTransactionCode() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "PAY" + TX_CODE_TIME.format(LocalDateTime.now()) + random;
    }

    private PaymentResponse toResponse(PaymentTransaction tx, String paymentUrl) {
        return PaymentResponse.builder()
                .id(tx.getId())
                .transactionCode(tx.getTransactionCode())
                .invoiceId(tx.getInvoiceId())
                .contractId(tx.getContractId())
                .tenantId(tx.getTenantId())
                .amount(tx.getAmount())
                .method(tx.getMethod())
                .status(tx.getStatus())
                .bankCode(tx.getBankCode())
                .gatewayTransactionNo(tx.getGatewayTransactionNo())
                .note(tx.getNote())
                .paymentUrl(paymentUrl)
                .paidAt(tx.getPaidAt())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
