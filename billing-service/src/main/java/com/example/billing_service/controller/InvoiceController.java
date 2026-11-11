package com.example.billing_service.controller;

import com.example.billing_service.dto.request.InvoiceCreateRequest;
import com.example.billing_service.dto.response.InvoiceDetailResponse;
import com.example.billing_service.dto.response.PageResponse;
import com.example.billing_service.dto.response.RevenueReportResponse;
import com.example.billing_service.exception.BadRequestException;
import com.example.billing_service.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final BillingService billingService;

    @GetMapping("{id}")
    @PreAuthorize("hasAnyRole('LANDLORD')")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(@PathVariable long id){
        return ResponseEntity.ok(billingService.getTotalAmount(id));
    }

    @PatchMapping("{id}/status")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<InvoiceDetailResponse> updateStatus(@PathVariable long id,
                                                              @RequestParam String status){
        return ResponseEntity.ok(billingService.updateInvoiceStatus(id, status));
    }
    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<InvoiceDetailResponse> createMonthlyInvoice(
            @Valid @RequestBody InvoiceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.createMonthlyInvoice(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'TENANT')")
    public ResponseEntity<PageResponse<InvoiceDetailResponse>> getInvoices(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            @AuthenticationPrincipal Jwt jwt) {

        String role = jwt.getClaimAsString("role");
        if ("LANDLORD".equals(role) || "ROLE_LANDLORD".equals(role)) {
            return ResponseEntity.ok(billingService.getInvoicesForLandLord(month, year, status, page, size, sort));
        }
        if ("TENANT".equals(role) || "ROLE_TENANT".equals(role)) {
            Long tenantId = jwt.getClaim("tenantId");
            if (tenantId == null) {
                throw new BadRequestException("Token không chứa thông tin tenantId.");
            }
            return ResponseEntity.ok(billingService.getInvoicesForTenant(tenantId, page, size));
        }
        throw new AccessDeniedException("Không có quyền truy cập danh sách hóa đơn.");
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/revenue-report")
    public ResponseEntity<RevenueReportResponse> revenueReport(@RequestParam(required = false) Integer month,
                                                               @RequestParam(required = false) Integer year) {
            return ResponseEntity.ok(billingService.getRevenueReport(month, year));
    }
}
