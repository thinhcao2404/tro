package com.example.identity_service.controller;

import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.dto.request.UpdateTenantAccountRequest;
import com.example.identity_service.dto.response.TenantAccountResponse;
import com.example.identity_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    @Autowired
    public AccountService accountService;

    @PostMapping("/tenants")
    public ResponseEntity<TenantAccountResponse> createTenantAccount(
            @Valid @RequestBody CreateTenantAccountRequest createTenantAccountRequest) {

        String generatedPassword = accountService.createTenantAccount(createTenantAccountRequest);

        TenantAccountResponse response = TenantAccountResponse.builder()
                .username(createTenantAccountRequest.getUsername())
                .password(generatedPassword)
                .message("Tạo tài khoản cho khách thuê thành công.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/tenants")
    public ResponseEntity<Void> updateTenantAccount(@RequestBody UpdateTenantAccountRequest request) {
        accountService.updateTenantAccount(request);
        return ResponseEntity.ok().build();
    }
}
