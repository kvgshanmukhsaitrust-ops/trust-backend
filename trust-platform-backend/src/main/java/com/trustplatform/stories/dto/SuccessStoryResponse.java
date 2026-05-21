package com.trustplatform.stories.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessStoryResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String category;
    private boolean published;
    private boolean featured;
    private int displayOrder;

    // --- New Enterprise Fields ---
    private String location;
    private String subtitle;
    private String beforeImageUrl;
    private String afterImageUrl;
    private String videoUrl;
    private String testimonialQuote;
    private String testimonialAuthor;

    private List<MilestoneResponse> timeline;
    private List<MetricResponse> metrics;
    private List<MediaAssetResponse> gallery;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneResponse {
        private Long id;
        private String date;
        private String title;
        private String description;
        private String imageUrl;
        private int orderIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResponse {
        private Long id;
        private String label;
        private String value;
        private String icon;
        private int displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaAssetResponse {
        private Long id;
        private String mediaType; // IMAGE, VIDEO
        private String url;
        private String thumbnailUrl;
        private String caption;
        private int orderIndex;
    }
}