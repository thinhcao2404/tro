package com.example.tenant_service.controller;

import com.example.tenant_service.dto.request.TenantRequest;
import com.example.tenant_service.dto.response.PageResponse;
import com.example.tenant_service.dto.response.TenantResponse;
import com.example.tenant_service.entity.TenantProfile;
import com.example.tenant_service.service.TenantService;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    @Autowired
    private TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<PageResponse<TenantResponse>> getAllTenants(@RequestParam(required = false) String search,
                                                                    @RequestParam(required = false) String phone,
                                                                    @RequestParam(required = false) String identityNumber,
                                                                    @RequestParam(required = false) Boolean isDeleted,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "4") int size,
                                                                    @RequestParam(defaultValue = "id,asc") String sort) {
        PageResponse<TenantResponse> tenants= tenantService.getTenants(search,phone,identityNumber,isDeleted,page,size,sort);
        return ResponseEntity.ok(tenants);
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable Long tenantId,
                                                       @Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.updateTenant(tenantId,request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<?> deleteTenant(@PathVariable Long tenantId){
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
