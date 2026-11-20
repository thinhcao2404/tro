package com.example.payment_service.dto.response;

import com.example.payment_service.enums.PaymentMethod;
import com.example.payment_service.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    Long id;
    String transactionCode;
    Long invoiceId;
    Long contractId;
    Long tenantId;
    BigDecimal amount;
    PaymentMethod method;
    PaymentStatus status;
    String bankCode;
    String gatewayTransactionNo;
    String note;
    String paymentUrl;
    LocalDateTime paidAt;
    LocalDateTime createdAt;
}
