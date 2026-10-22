package com.example.contract_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractCreateRequest {

    @NotNull(message = "Room ID không được để trống")
    Long roomId;

    @NotNull(message = "Tenant ID không được để trống")
    Long tenantId;

    @NotNull(message = "Tiền cọc không được để trống")
    @DecimalMin(value = "0.0", message = "Tiền cọc không được âm")
    BigDecimal deposit;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    LocalDate startDate;

    LocalDate endDate;
}