package com.trustplatform.applicant.dto;

import com.trustplatform.applicant.CaseCategory;
import com.trustplatform.applicant.CasePriority;
import com.trustplatform.applicant.CaseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CaseResponse {
    private Long id;
    private String caseNumber;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private CaseCategory category;
    private CaseStatus status;
    private CasePriority priority;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private String title;
    private String description;
    private String internalNotes;
    private String outcomeDetails;
    private String aiSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CaseDocumentResponse> documents;
    private List<CaseMilestoneResponse> milestones;

    // Personal Information
    private Integer age;
    private String gender;
    private LocalDate dateOfBirth;
    private String mobile;
    private String email;
    private String aadhaar;
    private String pan;

    // Address
    private String house;
    private String street;
    private String village;
    private String city;
    private String district;
    private String state;
    private String pincode;

    // Family Details
    private String occupation;
    private BigDecimal monthlyIncome;
    private Integer familyMembers;
    private Integer familyDependents;

    // Additional Tracking Details
    private BigDecimal estimatedCost;
    private String preferredContact;
    private LocalDateTime scheduledVisit;
    private BigDecimal approvedAmount;
    private BigDecimal disbursedAmount;
    private LocalDateTime slaDeadline;
    private Boolean escalated;
    private String committeeNotes;
    private String tags;
}
