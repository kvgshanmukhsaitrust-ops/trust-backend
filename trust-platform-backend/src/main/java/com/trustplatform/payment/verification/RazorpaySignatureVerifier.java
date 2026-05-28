package com.trustplatform.payment.verification;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RazorpaySignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static boolean verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature,
            String secret) {

        try {
            if (orderId == null || paymentId == null || signature == null || secret == null) {
                return false;
            }

            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance(HMAC_SHA256);

            SecretKeySpec secretKey =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);

            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            String generatedSignature =
                    Base64.getEncoder().encodeToString(hash);

            return generatedSignature.equals(signature);

        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            return false;
        }
    }
}