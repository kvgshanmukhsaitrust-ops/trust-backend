package com.trustplatform.volunteer.dto;

import com.trustplatform.volunteer.VolunteerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;     // Added to support the Builder
    private Long userId;
    private String userFullName;   // Added to support the Builder
    private VolunteerStatus status;
    private String message;
    
}