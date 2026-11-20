package com.example.payment_service.service;

import com.example.payment_service.config.VnPayConfig;
import com.example.payment_service.entity.PaymentTransaction;
import com.example.payment_service.exception.BadRequestException;
import com.example.payment_service.util.VnPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VnPayService {

    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayConfig vnPayConfig;

    public String buildPaymentUrl(PaymentTransaction tx, String clientIp) {
        if (isBlank(vnPayConfig.getTmnCode()) || isBlank(vnPayConfig.getHashSecret())) {
            throw new BadRequestException("Chưa cấu hình VNPAY_TMN_CODE hoặc VNPAY_HASH_SECRET.");
        }

        ZonedDateTime now = ZonedDateTime.now(VNPAY_ZONE);
        ZonedDateTime expireAt = now.plusMinutes(vnPayConfig.getExpireMinutes());
        long vnpAmount = tx.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        params.put("vnp_TxnRef", tx.getTransactionCode());
        params.put("vnp_OrderInfo", "Thanh toan hoa don #" + tx.getInvoiceId());
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", VNPAY_DATE_FORMAT.format(now));
        params.put("vnp_ExpireDate", VNPAY_DATE_FORMAT.format(expireAt));

        if (!isBlank(tx.getBankCode())) {
            params.put("vnp_BankCode", tx.getBankCode().trim());
        }

        String signedQuery = VnPayUtil.buildSignedQuery(params, vnPayConfig.getHashSecret());
        return vnPayConfig.getPayUrl() + "?" + signedQuery;
    }

    public boolean verifySignature(Map<String, String> params) {
        return VnPayUtil.verifySignature(params, vnPayConfig.getHashSecret());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
