package com.trustplatform.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {

    private Long donationId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}