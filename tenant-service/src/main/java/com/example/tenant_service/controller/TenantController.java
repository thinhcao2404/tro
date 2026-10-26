package com.example.tenant_service.controller;

import com.example.tenant_service.dto.request.TenantRequest;
import com.example.tenant_service.dto.response.TenantResponse;
import com.example.tenant_service.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    @Autowired
    private TenantService tenantService;

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable Long tenantId, @Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.updateTenant(tenantId,request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<?> deleteTenant(@PathVariable Long tenantId){
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
