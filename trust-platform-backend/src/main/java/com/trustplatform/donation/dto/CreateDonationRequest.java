package com.trustplatform.donation.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateDonationRequest {

    @NotNull
    @DecimalMin(value = "1.0", message = "Donation amount must be at least 1")
    private BigDecimal amount;

    @NotBlank
    private String donorName;

    @Email
    @NotBlank
    private String donorEmail;

    private String message;

    private Long eventId; // optional

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format. Must be 5 letters, 4 digits, 1 letter (e.g. ABCDE1234F).")
    private String donorPan;

    private String donorAddress;
}