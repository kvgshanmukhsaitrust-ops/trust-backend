package com.trustplatform.payment;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.payment.dto.CreateOrderResponse;
import com.trustplatform.payment.dto.VerifyPaymentRequest;
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

    @PostMapping("/create-order/{donationId}")
    @PreAuthorize("hasAnyAuthority('USER','VOLUNTEER','ADMIN')")
    public ResponseEntity<ApiSuccessResponse<CreateOrderResponse>> createOrder(
            @PathVariable Long donationId) throws Exception {

        CreateOrderResponse response = paymentService.createOrder(donationId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CreateOrderResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Order created successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiSuccessResponse<String>> verifyPayment(
            @RequestBody VerifyPaymentRequest request) throws Exception {

        paymentService.verifyPayment(request);

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Payment verified successfully")
                        .data("SUCCESS")
                        .build()
        );
    }
}