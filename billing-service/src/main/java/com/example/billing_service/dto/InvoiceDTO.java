package com.example.billing_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDTO {
    private Long id;
    private Long contractId;
    private String invoiceType;
    private Double amount;
    private String status;
    private String createdAt;
}