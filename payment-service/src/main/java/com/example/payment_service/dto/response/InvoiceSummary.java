package com.example.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceSummary(
        Long id,
        Long contractId,
        String roomNumber,
        BigDecimal totalAmount,
        String status
) {}
