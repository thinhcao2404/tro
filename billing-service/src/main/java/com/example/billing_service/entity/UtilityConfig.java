package com.example.billing_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "utility_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "service_code", nullable = false, unique = true, length = 30)
    String serviceCode;

    @Column(name = "service_name", nullable = false, length = 100)
    String serviceName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "unit_type", nullable = false, length = 20)
    String unitType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;
}