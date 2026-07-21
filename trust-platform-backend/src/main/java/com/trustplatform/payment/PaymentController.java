package com.trustplatform.payment;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.payment.dto.CreateOrderResponse;
import com.trustplatform.payment.dto.VerifyPaymentRequest;
import com.trustplatform.audit.AuditAction;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // =========================================
    // CREATE RAZORPAY ORDER
    // =========================================
    @PostMapping("/create-order/{donationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<CreateOrderResponse>> createOrder(
            @PathVariable Long donationId) throws Exception {

        CreateOrderResponse response = paymentService.createOrder(donationId);
        return ResponseEntity.ok(
                ApiSuccessResponse.<CreateOrderResponse>builder()
                        .timestamp(LocalDateTime.now()).status(200)
                        .message("Order created successfully").data(response).build());
    }

    // =========================================
    // VERIFY PAYMENT (client callback)
    // =========================================
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<String>> verifyPayment(
            @jakarta.validation.Valid @RequestBody VerifyPaymentRequest request) throws Exception {

        paymentService.verifyPayment(request);
        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now()).status(200)
                        .message("Payment verified successfully").data("SUCCESS").build());
    }

    // =========================================
    // ADMIN REFUND — STRICTLY CONTROLLED
    // Double-safety: MANAGE_SETTINGS + mandatory reason
    // =========================================
    @PostMapping("/admin/refund/{donationId}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS')")
    @AuditAction("ADMIN_DONATION_REFUND")
    public ResponseEntity<ApiSuccessResponse<String>> refundDonation(
            @PathVariable Long donationId,
            @RequestBody RefundRequest refundRequest) throws Exception {

        if (refundRequest.getReason() == null || refundRequest.getReason().isBlank()) {
            throw new IllegalArgumentException("Refund reason is mandatory for audit trail compliance.");
        }
        paymentService.refundDonation(donationId, refundRequest.getReason());
        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now()).status(200)
                        .message("Refund processed successfully for donation #" + donationId)
                        .data("REFUNDED").build());
    }

    // =========================================
    // Inner DTO for refund body
    // =========================================
    @Data
    public static class RefundRequest {
        private String reason;
    }
}