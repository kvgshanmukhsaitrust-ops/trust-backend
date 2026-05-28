package com.trustplatform.analytics.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventEngagementStat {
    private Long eventId;
    private String eventTitle;
    private String status;
    private int maxVolunteers;
    private long totalApplications;
    private double approvalRate;
}
