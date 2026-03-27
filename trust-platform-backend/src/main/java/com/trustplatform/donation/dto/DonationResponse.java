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
    private LocalDateTime createdAt;
}