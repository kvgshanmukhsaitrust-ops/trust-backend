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
import com.trustplatform.notification.NotificationService;
import com.trustplatform.payment.dto.CreateOrderResponse;
import com.trustplatform.payment.dto.VerifyPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trustplatform.payment.transaction.PaymentTransaction;
import com.trustplatform.payment.transaction.PaymentStatus;
import com.trustplatform.payment.transaction.PaymentTransactionRepository;
import com.trustplatform.user.UserRepository;
import com.trustplatform.user.User;
import com.trustplatform.user.Role;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClientService razorpayClientService;
    private final DonationRepository donationRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key-id:dummy}")
    private String keyId;

    @Value("${razorpay.key-secret:dummy}")
    private String keySecret;

    // ===============================
    // CREATE ORDER
    // ===============================
    @Transactional
    public CreateOrderResponse createOrder(Long donationId) throws Exception {

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        String orderId;
        boolean isSandbox = "dev_razorpay_key".equals(keyId) || "dummy".equals(keyId) || keyId == null || keyId.trim().isEmpty();
        // Sandbox checkout simulation mode check
        if (isSandbox) {
            orderId = "order_mock_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("[PaymentService] Simulated order created for Sandbox checkout: {}", orderId);
        } else {
            Order order = razorpayClientService.createOrder(donation.getAmount());
            orderId = order.get("id");
            log.info("[PaymentService] Real Razorpay order created on gateway: {}", orderId);
        }

        donation.setGatewayOrderId(orderId);
        donation.setStatus(DonationStatus.PROCESSING); // State Machine: transitioning to PROCESSING
        donationRepository.save(donation);

        // Record strictly append-only immutable event log entry in ledger
        PaymentTransaction tx = PaymentTransaction.builder()
                .correlationId("TX-CORR-" + UUID.randomUUID().toString())
                .donation(donation)
                .gateway("RAZORPAY")
                .gatewayOrderId(orderId)
                .amount(donation.getAmount())
                .status(PaymentStatus.CREATED)
                .metadata("{\"event\":\"ORDER_CREATED\",\"simulated\":" + (isSandbox ? "true" : "false") + "}")
                .build();
        paymentTransactionRepository.save(tx);

        return CreateOrderResponse.builder()
                .orderId(orderId)
                .amount(donation.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .currency("INR")
                .key(isSandbox ? "dummy" : keyId)
                .build();
    }

    // ===============================
    // VERIFY PAYMENT (FRONTEND)
    // ===============================
    @Transactional
    public synchronized void verifyPayment(VerifyPaymentRequest request) throws Exception {

        Donation donation = donationRepository.findById(request.getDonationId())
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        // Idempotency: duplicate click & processing protection
        if (donation.getStatus() == DonationStatus.SUCCESS) {
            log.info("[PaymentService] Donation id={} already processed as SUCCESS. Skipping double verification.", donation.getId());
            return;
        }

        boolean isMock = request.getRazorpayOrderId() != null && request.getRazorpayOrderId().startsWith("order_mock_");

        if (isMock) {
            log.info("[PaymentService] Verifying mock order sandbox transaction: {}", request.getRazorpayOrderId());
            donation.setPaymentMethod("Card (Sandbox)");
        } else {
            // Real cryptographic signature validation
            String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            String generatedSignature = hmacSha256Hex(payload, keySecret);

            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                donation.setStatus(DonationStatus.FAILED);
                donation.setErrorDetails("Cryptographic signature validation mismatch.");
                donationRepository.save(donation);
                log.error("[PaymentService] Payment signature verification failed for donation id={}", donation.getId());
                throw new RuntimeException("Invalid payment signature");
            }
            donation.setPaymentMethod("Razorpay Secured Gateway");
        }

        donation.setStatus(DonationStatus.SUCCESS); // State Machine: transitioning to SUCCESS
        donation.setTransactionId(request.getRazorpayPaymentId());
        donation.setReceiptNumber("REC-" + System.currentTimeMillis());
        donation.setGatewayResponse("HANDSHAKE_VERIFIED_" + (isMock ? "SANDBOX" : "LIVE"));

        donationRepository.save(donation);

        // Immutable financial ledger entry
        PaymentTransaction tx = PaymentTransaction.builder()
                .correlationId(donation.getCorrelationId() != null ? donation.getCorrelationId() : "TX-CORR-" + UUID.randomUUID().toString())
                .donation(donation)
                .gateway("RAZORPAY")
                .gatewayOrderId(request.getRazorpayOrderId())
                .gatewayPaymentId(request.getRazorpayPaymentId())
                .gatewaySignature(request.getRazorpaySignature())
                .amount(donation.getAmount())
                .paymentMethod(donation.getPaymentMethod())
                .status(PaymentStatus.VERIFIED)
                .metadata("{\"event\":\"VERIFY_PAYMENT_CALLBACK\",\"isMock\":" + isMock + "}")
                .build();
        paymentTransactionRepository.save(tx);

        // Realtime notifications sync & Email Dispatch
        triggerDonationSuccessEvents(donation);
    }

    // ===============================
    // WEBHOOK HANDLER
    // ===============================
    @Transactional
    public synchronized void handleWebhook(String signature, String payload) throws Exception {

        String generatedSignature = hmacSha256Hex(payload, keySecret);

        if (!generatedSignature.equals(signature)) {
            log.error("[PaymentService] Webhook processing failed: Invalid signature.");
            throw new RuntimeException("Invalid webhook signature");
        }

        JsonNode root = objectMapper.readTree(payload);
        String event = root.get("event").asText();

        if ("payment.captured".equals(event) || "payment.failed".equals(event)) {

            JsonNode entity = root
                    .get("payload")
                    .get("payment")
                    .get("entity");

            String orderId = entity.get("order_id").asText();
            String paymentId = entity.get("id").asText();

            Donation donation = donationRepository
                    .findByGatewayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

            // Duplicate processing / idempotency protection
            if (donation.getStatus() == DonationStatus.SUCCESS) {
                log.info("[PaymentService] Webhook received for already successful donation id={}. Skipping duplicate processing.", donation.getId());
                return;
            }

            if ("payment.captured".equals(event)) {
                donation.setStatus(DonationStatus.SUCCESS); // State Machine: transitioning to SUCCESS
                donation.setTransactionId(paymentId);
                donation.setReceiptNumber("REC-" + System.currentTimeMillis());
                donation.setPaymentMethod(entity.has("method") ? entity.get("method").asText() : "Razorpay Webhook");
                donation.setGatewayResponse("WEBHOOK_CONFIRMED_CAPTURED");

                donationRepository.save(donation);

                // Immutable transaction ledger entry
                PaymentTransaction tx = PaymentTransaction.builder()
                        .correlationId(donation.getCorrelationId() != null ? donation.getCorrelationId() : "TX-CORR-" + UUID.randomUUID().toString())
                        .donation(donation)
                        .gateway("RAZORPAY")
                        .gatewayOrderId(orderId)
                        .gatewayPaymentId(paymentId)
                        .amount(donation.getAmount())
                        .paymentMethod(donation.getPaymentMethod())
                        .status(PaymentStatus.SUCCESS)
                        .metadata("{\"event\":\"WEBHOOK_CAPTURED\",\"payload_captured\":" + entity.toString() + "}")
                        .build();
                paymentTransactionRepository.save(tx);

                triggerDonationSuccessEvents(donation);
                log.info("[PaymentService] Webhook confirmed payment captured successfully for donation id={}", donation.getId());
            } else {
                // payment.failed event
                donation.setStatus(DonationStatus.FAILED); // State Machine: transitioning to FAILED
                if (entity.has("error_description")) {
                    donation.setErrorDetails(entity.get("error_description").asText());
                }
                donation.setGatewayResponse("WEBHOOK_CONFIRMED_FAILED");
                donationRepository.save(donation);

                // Immutable transaction ledger entry for failure
                PaymentTransaction tx = PaymentTransaction.builder()
                        .correlationId(donation.getCorrelationId() != null ? donation.getCorrelationId() : "TX-CORR-" + UUID.randomUUID().toString())
                        .donation(donation)
                        .gateway("RAZORPAY")
                        .gatewayOrderId(orderId)
                        .gatewayPaymentId(paymentId)
                        .amount(donation.getAmount())
                        .status(PaymentStatus.FAILED)
                        .errorDetails(donation.getErrorDetails())
                        .metadata("{\"event\":\"WEBHOOK_FAILED\",\"payload_captured\":" + entity.toString() + "}")
                        .build();
                paymentTransactionRepository.save(tx);

                log.warn("[PaymentService] Webhook confirmed payment failure for donation id={}", donation.getId());
            }
        }
    }

    // ===============================
    // NOTIFICATIONS & AUDIT LEDGER
    // ===============================
    private void triggerDonationSuccessEvents(Donation donation) {
        // 1. Send Email Receipt
        sendDonationEmail(donation);

        // 2. Promotion: If user makes a donation, they become a DONOR
        try {
            if (donation.getUser() != null) {
                User user = donation.getUser();
                if (user.getRole() == Role.USER) {
                    user.setRole(Role.DONOR);
                    userRepository.save(user);
                    log.info("[PaymentService] User id={} successfully promoted to DONOR.", user.getId());
                }
            }
        } catch (Exception e) {
            log.error("[PaymentService] Failed to promote user to DONOR: {}", e.getMessage());
        }

        // 3. Trigger WebSocket Real-time Stomp Alerts
        try {
            notificationService.sendToAdmins("Successful Donation Received", 
                    "A successful donation of ₹" + donation.getAmount() + " was received from " + donation.getDonorName() + "!", "DONATION", donation.getCorrelationId());

            if (donation.getUser() != null) {
                notificationService.sendToUser(donation.getUser().getEmail(), "Donation Successful", 
                        "Thank you for your generous contribution of ₹" + donation.getAmount() + "! Your receipt number is " + donation.getReceiptNumber(), "DONATION", donation.getCorrelationId());
            }
        } catch (Exception e) {
            log.error("[PaymentService] Failed to dispatch WebSocket notifications: {}", e.getMessage());
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

    // ===============================
    // ADMIN REFUND OPERATION (strictly controlled)
    // ===============================
    @Transactional
    public synchronized void refundDonation(Long donationId, String reason) throws Exception {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        // SAFETY GATE 1: Only SUCCESS donations can be refunded
        if (donation.getStatus() != DonationStatus.SUCCESS) {
            throw new IllegalStateException(
                "Only SUCCESS donations can be refunded. Current status: " + donation.getStatus());
        }

        // SAFETY GATE 2: Prevent duplicate refunds (idempotency)
        if (donation.getRefundDate() != null) {
            throw new IllegalStateException(
                "Donation id=" + donationId + " was already refunded on " + donation.getRefundDate());
        }

        boolean isMock = donation.getGatewayOrderId() != null
                && donation.getGatewayOrderId().startsWith("order_mock_");

        if (!isMock && donation.getTransactionId() != null) {
            // Live Razorpay refund
            try {
                org.json.JSONObject refundOptions = new org.json.JSONObject();
                refundOptions.put("speed", "normal");
                razorpayClientService.getRazorpayClient().payments
                        .refund(donation.getTransactionId(), refundOptions);
                log.info("[PaymentService] Razorpay refund initiated for paymentId={}", donation.getTransactionId());
            } catch (Exception e) {
                log.error("[PaymentService] Razorpay refund API call failed: {}", e.getMessage());
                throw new RuntimeException("Refund gateway call failed: " + e.getMessage());
            }
        } else {
            log.info("[PaymentService] Sandbox mock refund processed for donationId={}", donationId);
        }

        // Transition to terminal REFUNDED state
        donation.setStatus(DonationStatus.REFUNDED);
        donation.setRefundReason(reason);
        donation.setRefundDate(java.time.LocalDateTime.now());
        donation.setGatewayResponse("REFUNDED_" + (isMock ? "SANDBOX" : "LIVE") + "_" + System.currentTimeMillis());
        donationRepository.save(donation);

        // Immutable transaction ledger entry for refund
        PaymentTransaction tx = PaymentTransaction.builder()
                .correlationId(donation.getCorrelationId() != null ? donation.getCorrelationId() : "TX-CORR-" + UUID.randomUUID().toString())
                .donation(donation)
                .gateway("RAZORPAY")
                .gatewayOrderId(donation.getGatewayOrderId())
                .gatewayPaymentId(donation.getTransactionId())
                .amount(donation.getAmount())
                .status(PaymentStatus.REFUNDED)
                .errorDetails(reason)
                .metadata("{\"event\":\"ADMIN_REFUND_PROCESSED\",\"isMock\":" + isMock + "}")
                .build();
        paymentTransactionRepository.save(tx);

        // Notify admins and donor
        try {
            notificationService.sendToAdmins("Donation Refunded",
                    "Refund of \u20b9" + donation.getAmount() + " processed for donation #" + donationId
                    + ". Reason: " + reason, "REFUND", donation.getCorrelationId());
            if (donation.getUser() != null) {
                notificationService.sendToUser(donation.getUser().getEmail(), "Refund Processed",
                        "Your donation of \u20b9" + donation.getAmount() + " has been refunded. Reason: " + reason,
                        "REFUND", donation.getCorrelationId());
            }
        } catch (Exception e) {
            log.error("[PaymentService] Refund notification dispatch failed: {}", e.getMessage());
        }

        log.info("[PaymentService] Donation id={} transitioned to REFUNDED successfully", donationId);
    }
}