package com.trustplatform.analytics.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignStat {
    private Long eventId;
    private String eventTitle;
    private BigDecimal totalRaised;
    private long donationCount;
}
