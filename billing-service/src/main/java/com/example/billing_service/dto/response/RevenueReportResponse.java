package com.example.billing_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportResponse {
    Integer month;
    Integer year;
    Long totalPaidInvoices;
    BigDecimal totalRoomRevenue;
    BigDecimal totalElecRevenue;
    BigDecimal totalWaterRevenue;
    BigDecimal totalOtherRevenue;
    BigDecimal grandTotalRevenue;
}