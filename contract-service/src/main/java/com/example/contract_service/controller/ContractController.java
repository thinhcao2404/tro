package com.example.contract_service.controller;

import com.example.contract_service.dto.*;
import com.example.contract_service.dto.request.ContractCreateRequest;
import com.example.contract_service.dto.response.ContractResponse;
import com.example.contract_service.dto.response.PageResponse;
import com.example.contract_service.entity.ContractAgreement;
import com.example.contract_service.enums.ContractStatus;
import com.example.contract_service.exception.BadRequestException;
import com.example.contract_service.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {
    @Autowired
    private ContractService contractService;

    @PatchMapping("/{id}/checkout")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractResponse> cancelContract(@PathVariable Long id){
        ContractResponse response = contractService.cancelContract(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkin")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody ContractCreateRequest contractCreateRequest){
        ContractResponse response = contractService.createContract(contractCreateRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("internal/check-active")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Boolean> getActiveContract(@RequestParam("tenantId") Long tenantId){
       boolean hasActive = contractService.hasActiveContract(tenantId);
       return ResponseEntity.ok(hasActive);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ContractResponse>> getContracts(
            @RequestParam(required = false)  String search,
            @RequestParam(required = false)  String status,
            @RequestParam(required = false)  BigDecimal deposit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        PageResponse<ContractResponse> response = contractService.getContracts(search, status, deposit, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-contracts")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<ContractResponse>> getMyContracts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)ContractStatus status) {
        Long tenantId = jwt.getClaim("tenantId");
        if (tenantId == null) {
            throw new BadRequestException("Token không chứa thông tin tenantId.");
        }
        List<ContractResponse> response = contractService.getContract(tenantId,status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{requestedTenantId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<ContractResponse>> getContractByTenantId(
            @PathVariable Long requestedTenantId,
            @RequestParam ContractStatus status) {

        List<ContractResponse> response = contractService.getContract(requestedTenantId,status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("{contractId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ContractResponse> getContractById(
            @PathVariable Long contractId){
        return ResponseEntity.ok(contractService.findContractById(contractId));
    }
}