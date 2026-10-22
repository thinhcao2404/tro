package com.example.billing_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "contract_id", nullable = false)
    Long contractId;

    @Column(name = "month", nullable = false)
    Integer month;

    @Column(name = "year", nullable = false)
    Integer year;

    @Column(name = "old_elec", nullable = false, precision = 8, scale = 2)
    BigDecimal oldElec;

    @Column(name = "new_elec", nullable = false, precision = 8, scale = 2)
    BigDecimal newElec;

    @Column(name = "elec_cost", nullable = false, precision = 12, scale = 2)
    BigDecimal elecCost;

    @Column(name = "water_cost", nullable = false, precision = 12, scale = 2)
    BigDecimal waterCost;

    @Column(name = "room_cost", nullable = false, precision = 12, scale = 2)
    BigDecimal roomCost;

    @Column(name = "other_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    BigDecimal otherCost = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    String status = "UNPAID";

    @Column(name = "payment_date")
    LocalDateTime paymentDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}