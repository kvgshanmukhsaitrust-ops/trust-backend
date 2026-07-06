package com.trustplatform.applicant.dto;

import com.trustplatform.applicant.CaseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull(message = "New status is required")
    private CaseStatus status;
    private String comment;
}
