package com.example.billing_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityConfigRequest {

    @NotBlank(message = "Mã dịch vụ không được để trống")
    String serviceCode;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    String serviceName;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    BigDecimal unitPrice;

    @NotBlank(message = "Đơn vị tính không được để trống")
    String unitType;

    @Builder.Default
    Boolean isActive = true;
}