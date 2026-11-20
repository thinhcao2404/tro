package com.example.payment_service.dto.event;

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
public class PaymentCompletedEvent {
    private String eventId;
    private String transactionCode;
    private Long invoiceId;
    private Long contractId;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
}
