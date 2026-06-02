package com.trustplatform.payment.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustplatform.donation.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final DonationService donationService;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook-secret:dummy_secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload) {

        if (signature == null || signature.trim().isEmpty()) {
            log.error("[RazorpayWebhookController] Webhook signature missing in headers");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Signature missing");
        }

        try {
            // HMAC SHA-256 signature validation
            String generatedSignature = hmacSha256Hex(payload, webhookSecret);
            if (!generatedSignature.equals(signature)) {
                log.error("[RazorpayWebhookController] Webhook signature validation mismatch!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            
            // Webhook Replay Attack Prevention
            if (!root.has("id")) {
                log.error("[RazorpayWebhookController] Webhook payload missing event ID");
                return ResponseEntity.badRequest().body("Event ID missing");
            }
            String eventId = root.get("id").asText();
            if (processedWebhookEventRepository.existsById(eventId)) {
                log.warn("[RazorpayWebhookController] Duplicate webhook event detected (Replay Attack Prevention): {}", eventId);
                return ResponseEntity.ok("Duplicate event - already processed");
            }

            // Save processed event ID to secure ledger database
            processedWebhookEventRepository.save(new ProcessedWebhookEvent(eventId));

            String event = root.get("event").asText();

            // Process payment.captured events only
            if ("payment.captured".equals(event)) {
                JsonNode paymentEntity = root.get("payload").get("payment").get("entity");
                
                String payerEmail = paymentEntity.has("email") && !paymentEntity.get("email").isNull() 
                        ? paymentEntity.get("email").asText() 
                        : null;
                
                if (payerEmail == null || payerEmail.trim().isEmpty()) {
                    log.warn("[RazorpayWebhookController] Webhook captured payment missing email. Skipping role progression.");
                    return ResponseEntity.ok("Payment captured but email missing");
                }

                long amountInPaise = paymentEntity.get("amount").asLong();
                java.math.BigDecimal amount = java.math.BigDecimal.valueOf(amountInPaise)
                        .divide(java.math.BigDecimal.valueOf(100));
                
                String transactionId = paymentEntity.get("id").asText();
                String gatewayOrderId = paymentEntity.has("order_id") && !paymentEntity.get("order_id").isNull()
                        ? paymentEntity.get("order_id").asText()
                        : null;
                
                String metadata = paymentEntity.has("notes") && !paymentEntity.get("notes").isNull()
                        ? paymentEntity.get("notes").toString()
                        : "{}";

                donationService.processVerifiedDonation(payerEmail, amount, transactionId, gatewayOrderId, metadata);
                log.info("[RazorpayWebhookController] Successfully verified and processed payment.captured for event ID: {}", eventId);
            } else {
                log.info("[RazorpayWebhookController] Ignored non-payment.captured event type: {}", event);
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("[RazorpayWebhookController] Webhook processing exception: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed: " + e.getMessage());
        }
    }

    private String hmacSha256Hex(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey =
                new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}