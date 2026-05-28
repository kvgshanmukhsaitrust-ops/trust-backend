package com.trustplatform.analytics.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsSummaryResponse {
    private DonorStat donorAnalytics;
    private List<CampaignStat> campaignAnalytics;
    private List<EventEngagementStat> eventEngagement;
    
    // Volunteer metrics
    private long totalVolunteersCount;
    private double volunteerApprovedRatio;
    private double volunteerPendingRatio;
    
    // Media metrics
    private long imageCount;
    private long videoCount;
    private double averageMediaItemsPerContent;
    
    // Story Performance metrics
    private long publishedStoriesCount;
    private long draftStoriesCount;
    private double averageVersionsPerStory;
}
