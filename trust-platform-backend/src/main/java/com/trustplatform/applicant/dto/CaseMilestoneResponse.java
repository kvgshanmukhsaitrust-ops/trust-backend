package com.trustplatform.applicant.dto;

import com.trustplatform.applicant.CaseStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CaseMilestoneResponse {
    private Long id;
    private String title;
    private String description;
    private CaseStatus statusSnapshot;
    private LocalDateTime timestamp;
}
