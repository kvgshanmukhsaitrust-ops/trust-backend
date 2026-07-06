package com.trustplatform.applicant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaseMessageRequest {
    @NotBlank(message = "Message content is required")
    private String messageContent;
    private boolean isInternal = false;
}
