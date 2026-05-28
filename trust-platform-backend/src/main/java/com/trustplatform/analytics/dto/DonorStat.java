package com.trustplatform.analytics.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DonorStat {
    private long totalDonorsCount;
    private long repeatDonorsCount;
    private BigDecimal averageDonationSize;
    private Map<String, Long> paymentMethodBreakdown;
}
