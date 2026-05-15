package com.trustplatform.impact.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImpactStatResponse {
    private Long id;
    private String category;
    private Long currentValue;
    private String unit;
    private String icon;
    private boolean featured;
    private int displayOrder;
}