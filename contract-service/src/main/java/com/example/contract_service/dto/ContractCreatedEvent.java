package com.example.contract_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractCreatedEvent {
    private Long contractId;
    private Long roomId;
    private Long tenantId;
    private BigDecimal deposit;
    private LocalDate startDate;
}
