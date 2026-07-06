package com.trustplatform.volunteer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerStatsResponse {
    private Double totalHoursServed;
    private Long totalEventsAttended;
    private Long rank;
    private List<String> badges;
    private String tier;
    private Double impactScore;
}
