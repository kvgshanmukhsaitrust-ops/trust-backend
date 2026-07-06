package com.trustplatform.applicant.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CaseMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String messageContent;
    private LocalDateTime sentAt;
    private boolean isInternal;
}
