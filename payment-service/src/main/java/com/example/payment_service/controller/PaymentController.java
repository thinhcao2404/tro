package com.example.payment_service.controller;

import com.example.payment_service.dto.request.PaymentConfirmRequest;
import com.example.payment_service.dto.request.PaymentCreateRequest;
import com.example.payment_service.dto.response.PageResponse;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.dto.response.VnPayIpnResponse;
import com.example.payment_service.dto.response.VnPayReturnResponse;
import com.example.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'TENANT')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request, jwt, httpRequest));
    }

    @PatchMapping("/{transactionCode}/confirm")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable String transactionCode,
            @Valid @RequestBody(required = false) PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(transactionCode, request));
    }

    @PatchMapping("/{transactionCode}/cancel")
    @PreAuthorize("hasAnyRole('LANDLORD', 'TENANT')")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable String transactionCode,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paymentService.cancelPayment(transactionCode, jwt));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<VnPayIpnResponse> vnPayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnPayIpn(params));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<VnPayReturnResponse> vnPayReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnPayReturn(params));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'TENANT')")
    public ResponseEntity<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paymentService.getPayments(invoiceId, status, method, page, size, sort, jwt));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'TENANT')")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paymentService.getPaymentById(id, jwt));
    }
}
