package com.example.billing_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractCreatedEvent {
    private String event;
    private Long contractId;
    private Long roomId;
    private Long tenantId;
    private Double deposit;
}