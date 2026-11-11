package com.example.billing_service.controller;

import com.example.billing_service.dto.request.UtilityConfigRequest;
import com.example.billing_service.entity.UtilityConfig;
import com.example.billing_service.service.UtilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utilities")
@RequiredArgsConstructor
public class UtilityController {
    private final UtilityService utilityService;

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping
    public ResponseEntity<List<UtilityConfig>> getUtilityConfigs() {
        return ResponseEntity.ok(utilityService.findAll());
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @PostMapping
    public ResponseEntity<UtilityConfig> saveUtilityConfig(@Valid @RequestBody UtilityConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilityService.createUtilityConfig(request));
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @PutMapping("{id}")
    public ResponseEntity<UtilityConfig> updateUtilityConfig(@PathVariable Long id,
                                                             @Valid @RequestBody UtilityConfigRequest request) {
        return ResponseEntity.ok(utilityService.updateUtilityConfig(id, request));
    }
}
