package com.example.billing_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentCompletedEvent {
    private String eventId;
    private String transactionCode;
    private Long invoiceId;
    private Long contractId;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
}
