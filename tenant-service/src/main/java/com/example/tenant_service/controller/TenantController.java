package com.example.tenant_service.controller;

import com.example.tenant_service.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    @Autowired
    private TenantService tenantService;

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<?> deleteTenant(@PathVariable Long tenantId){
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }
}
