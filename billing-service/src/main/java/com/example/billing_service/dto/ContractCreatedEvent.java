package com.example.billing_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractCreatedEvent {
    private String event;
    private Long contractId;
    private Long roomId;
    private Long tenantId;
    private Double deposit;
}