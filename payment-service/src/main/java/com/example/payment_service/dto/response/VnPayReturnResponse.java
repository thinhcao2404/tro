package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VnPayReturnResponse {
    private boolean validSignature;
    private boolean success;
    private String transactionCode;
    private BigDecimal amount;
    private String responseCode;
    private String transactionStatus;
    private String gatewayTransactionNo;
    private String message;
}
