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
public class ContractDetailResponse {
    Long id;

    // Thông tin lấy từ Property Service
    Long roomId;
    String roomNumber;
    Integer floor;
    BigDecimal roomPrice;

    // Thông tin lấy từ Tenant Service
    Long tenantId;
    String tenantName;
    String tenantPhone;
    String tenantIdentityNumber;

    // Thông tin Hợp đồng
    BigDecimal deposit;
    LocalDate startDate;
    LocalDate endDate;
    String status;
    LocalDateTime createdAt;
}