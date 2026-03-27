package com.trustplatform.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {

    // Donations
    private BigDecimal totalAmountCollected;
    private Long totalDonations;
    private Long successfulDonations;
    private Long pendingDonations;

    // Events
    private Long totalEvents;
    private Long upcomingEvents;
    private Long completedEvents;

    // Volunteers
    private Long totalApplications;
    private Long approvedVolunteers;
    private Long pendingApplications;

    // Monthly stats
    private List<MonthlyDonationStats> monthlyDonations;
}