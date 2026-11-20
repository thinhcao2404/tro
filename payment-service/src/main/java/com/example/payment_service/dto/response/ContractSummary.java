package com.example.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractSummary(
        Long id,
        Long roomId,
        Long tenantId,
        String status
) {}
