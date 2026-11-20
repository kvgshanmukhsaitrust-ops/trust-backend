package com.trustplatform.donation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Privacy-safe view of a successful donation for public display.
 * Never exposes email, PAN, address, message or gateway details.
 */
public record PublicDonorEntry(String name, BigDecimal amount, LocalDateTime donatedAt) {
}
