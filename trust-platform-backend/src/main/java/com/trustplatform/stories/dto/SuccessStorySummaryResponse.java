package com.trustplatform.stories.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuccessStorySummaryResponse {
    private Long id;
    private String title;
    private String category;
    private boolean published;
    private boolean featured;
    private String imageUrl;
    private String location;
    private String subtitle;
    private int displayOrder;
}
