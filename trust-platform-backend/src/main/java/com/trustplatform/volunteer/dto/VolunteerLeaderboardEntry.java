package com.trustplatform.volunteer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerLeaderboardEntry {
    private Long userId;
    private String userFullName;
    private String avatarUrl;
    private Double totalHoursServed;
    private Long totalEventsAttended;
    private Long rank;
}
