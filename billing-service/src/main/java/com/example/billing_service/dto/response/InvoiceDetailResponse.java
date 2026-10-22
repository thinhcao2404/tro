package com.example.billing_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetailResponse {
    Long id;
    Long contractId;
    String roomNumber;
    String tenantName;
    Integer month;
    Integer year;

    BigDecimal oldElec;
    BigDecimal newElec;
    BigDecimal elecCost;
    BigDecimal waterCost;
    BigDecimal roomCost;
    BigDecimal otherCost;
    BigDecimal totalAmount;

    String status;
    String paymentCode;
    String vietQrUrl;
    LocalDateTime paymentDate;
    LocalDateTime createdAt;
}