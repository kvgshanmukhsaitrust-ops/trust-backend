package com.trustplatform.applicant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseDocumentResponse {
    private Long id;
    private String documentName;
    private String documentUrl;
    private String fileType;
}
