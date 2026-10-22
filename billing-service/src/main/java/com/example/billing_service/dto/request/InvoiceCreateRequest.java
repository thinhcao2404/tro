package com.example.billing_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCreateRequest {

    @NotNull(message = "Contract ID không được để trống")
    Long contractId;

    @NotNull(message = "Tháng không được để trống")
    @Min(value = 1, message = "Tháng từ 1 đến 12")
    @Max(value = 12, message = "Tháng từ 1 đến 12")
    Integer month;

    @NotNull(message = "Năm không được để trống")
    @Min(value = 2020, message = "Năm không hợp lệ")
    Integer year;

    @NotNull(message = "Chỉ số điện cũ không được để trống")
    @DecimalMin(value = "0.0", message = "Chỉ số điện cũ không được âm")
    BigDecimal oldElec;

    @NotNull(message = "Chỉ số điện mới không được để trống")
    @DecimalMin(value = "0.0", message = "Chỉ số điện mới không được âm")
    BigDecimal newElec;

    @NotNull(message = "Tiền nước (hoặc số khối nước) không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trị không được âm")
    BigDecimal waterUsageOrCost;

    @Builder.Default
    BigDecimal otherCost = BigDecimal.ZERO;
}