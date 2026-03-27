package com.trustplatform.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationStatus;
import com.trustplatform.email.EmailService;
import com.trustplatform.email.EmailTemplateBuilder;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.payment.dto.CreateOrderResponse;
import com.trustplatform.payment.dto.VerifyPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClientService razorpayClientService;
    private final DonationRepository donationRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    @Value("${razorpay.key-id:dummy}")
    private String keyId;

    @Value("${razorpay.key-secret:dummy}")
    private String keySecret;

    // ===============================
    // CREATE ORDER
    // ===============================
    public CreateOrderResponse createOrder(Long donationId) throws Exception {

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        Order order = razorpayClientService.createOrder(donation.getAmount());
        String orderId = order.get("id");

        donation.setGatewayOrderId(orderId);
        donationRepository.save(donation);

        return CreateOrderResponse.builder()
                .orderId(orderId)
                .amount(donation.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .currency("INR")
                .key(keyId)
                .build();
    }

    // ===============================
    // VERIFY PAYMENT (FRONTEND)
    // ===============================
    public void verifyPayment(VerifyPaymentRequest request) throws Exception {

        Donation donation = donationRepository.findById(request.getDonationId())
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        if (donation.getStatus() == DonationStatus.SUCCESS) {
            return;
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String generatedSignature = hmacSha256Hex(payload, keySecret);

        if (!generatedSignature.equals(request.getRazorpaySignature())) {
            throw new RuntimeException("Invalid payment signature");
        }

        donation.setStatus(DonationStatus.SUCCESS);
        donation.setTransactionId(request.getRazorpayPaymentId());
        donation.setReceiptNumber("REC-" + System.currentTimeMillis());

        donationRepository.save(donation);

        sendDonationEmail(donation);
    }

    // ===============================
    // WEBHOOK HANDLER
    // ===============================
    public void handleWebhook(String signature, String payload) throws Exception {

        String generatedSignature = hmacSha256Hex(payload, keySecret);

        if (!generatedSignature.equals(signature)) {
            throw new RuntimeException("Invalid webhook signature");
        }

        JsonNode root = objectMapper.readTree(payload);
        String event = root.get("event").asText();

        if ("payment.captured".equals(event)) {

            JsonNode entity = root
                    .get("payload")
                    .get("payment")
                    .get("entity");

            String orderId = entity.get("order_id").asText();
            String paymentId = entity.get("id").asText();

            Donation donation = donationRepository
                    .findByGatewayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

            if (donation.getStatus() != DonationStatus.SUCCESS) {

                donation.setStatus(DonationStatus.SUCCESS);
                donation.setTransactionId(paymentId);
                donation.setReceiptNumber("REC-" + System.currentTimeMillis());

                donationRepository.save(donation);

                sendDonationEmail(donation);
            }
        }
    }

    // ===============================
    // EMAIL
    // ===============================
    private void sendDonationEmail(Donation donation) {

        try {
            String body = emailTemplateBuilder.buildDonationSuccessEmail(
                    donation.getDonorName(),
                    donation.getReceiptNumber()
            );

            emailService.sendEmail(
                    donation.getDonorEmail(),
                    "Donation Receipt Confirmation",
                    body
            );

        } catch (Exception e) {
            log.error("Failed to send donation email: {}", e.getMessage());
        }
    }

    // ===============================
    // HMAC SHA256 HEX
    // ===============================
    private String hmacSha256Hex(String data, String secret) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey =
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        mac.init(secretKey);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}