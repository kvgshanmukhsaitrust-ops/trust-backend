package com.trustplatform.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MonthlyDonationStats {

    private String month;
    private BigDecimal totalAmount;
}