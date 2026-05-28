package com.trustplatform.donation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trustplatform.donation.DonationStatus;

@Getter
@Builder
public class DonationResponse {

    private Long id;
    private BigDecimal amount;
    private String donorName;
    private String donorEmail;
    private String message;
    private DonationStatus status;
    private String receiptNumber;
    private String receiptUuid;
    private String receiptPdfPath;
    private LocalDateTime createdAt;
    private LocalDateTime refundDate;
    private String refundReason;
    private String paymentMethod;
    private String transactionId;
    private String eventTitle;
    // PAN is always masked - never expose raw PAN in response
    private String donorPanMasked; // e.g. XXXXX1234F
    private boolean hasPan;
    private boolean hasAddress;
    private String correlationId;
}
