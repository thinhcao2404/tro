package com.example.identity_service.controller;

import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class IdentifyController {
    @Autowired
    public AccountService accountService;

    @PostMapping("/tenants")
    public ResponseEntity<String> createTenantAccount(@Valid @RequestBody CreateTenantAccountRequest createTenantAccountRequest){
        accountService.createTenantAccount(createTenantAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("Tạo tài khoản cho khách thuê thành công");
    }
}
