package com.example.contract_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractCreatedEvent implements Serializable {
    private Long contractId;
    private Long roomId;
    private Long tenantId;
    private Double deposit;
    private String startDate;
}