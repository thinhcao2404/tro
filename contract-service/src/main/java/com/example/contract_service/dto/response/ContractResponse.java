package com.example.contract_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    Long id;
    Long roomId;
    Long tenantId;
    BigDecimal deposit;
    LocalDate startDate;
    LocalDate endDate;
    String status;
    LocalDateTime createdAt;
}