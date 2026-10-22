package com.example.billing_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityConfigResponse {
    Long id;
    String serviceCode;
    String serviceName;
    BigDecimal unitPrice;
    String unitType;
    Boolean isActive;
}