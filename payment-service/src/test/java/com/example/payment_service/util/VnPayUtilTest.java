package com.example.payment_service.util;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VnPayUtilTest {

    @Test
    void buildSignedQueryAndVerifySignature_roundTripSucceeds() {
        String secret = "TEST_VNPAY_SECRET_KEY_123456";
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "TESTTMN1");
        params.put("vnp_Amount", "350000000");
        params.put("vnp_TxnRef", "PAY20261120001");
        params.put("vnp_OrderInfo", "Thanh toan hoa don #15");

        String query = VnPayUtil.buildSignedQuery(params, secret);
        Map<String, String> parsed = parseQuery(query);
        parsed.put("vnp_ResponseCode", "00");
        parsed.put("vnp_TransactionStatus", "00");

        // Re-sign with response params like VNPay IPN does
        String ipnQuery = VnPayUtil.buildSignedQuery(parsed, secret);
        Map<String, String> ipnParams = parseQuery(ipnQuery);

        assertTrue(VnPayUtil.verifySignature(ipnParams, secret));
    }

    @Test
    void verifySignature_failsWhenTamperedAmount() {
        String secret = "TEST_VNPAY_SECRET_KEY_123456";
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "TESTTMN1");
        params.put("vnp_Amount", "350000000");
        params.put("vnp_TxnRef", "PAY20261120001");

        Map<String, String> parsed = parseQuery(VnPayUtil.buildSignedQuery(params, secret));
        parsed.put("vnp_Amount", "100000");

        assertFalse(VnPayUtil.verifySignature(parsed, secret));
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.US_ASCII);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.US_ASCII);
                result.put(k, v);
            }
        }
        return result;
    }
}
