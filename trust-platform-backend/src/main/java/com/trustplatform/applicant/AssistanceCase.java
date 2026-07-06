package com.trustplatform.applicant;

import com.trustplatform.common.BaseAuditableEntity;
import com.trustplatform.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "assistance_cases",
        indexes = {
                @Index(name = "idx_case_status", columnList = "status"),
                @Index(name = "idx_case_category", columnList = "category"),
                @Index(name = "idx_case_priority", columnList = "priority"),
                @Index(name = "idx_case_number", columnList = "caseNumber", unique = true)
        }
)
public class AssistanceCase extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CaseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CaseStatus status = CaseStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    // --- Enterprise personal information ---
    private Integer age;
    private String gender;
    private LocalDate dateOfBirth;
    private String mobile;
    private String email;
    private String aadhaar;
    private String pan;

    // --- Enterprise address information ---
    private String house;
    private String street;
    private String village;
    private String city;
    private String district;
    private String state;
    private String pincode;

    // --- Enterprise family and financials ---
    private String occupation;
    private BigDecimal monthlyIncome;
    private Integer familyMembers;
    private Integer familyDependents;

    // --- Additional requirements ---
    private BigDecimal estimatedCost;
    private String preferredContact;

    // --- Case Tracking & Allocation ---
    private LocalDateTime scheduledVisit;
    private BigDecimal approvedAmount;
    private BigDecimal disbursedAmount;
    private LocalDateTime slaDeadline;

    @Builder.Default
    private Boolean escalated = false;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @Column(columnDefinition = "TEXT")
    private String committeeNotes;

    private String tags;

    @Column(columnDefinition = "TEXT")
    private String outcomeDetails;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @OneToMany(mappedBy = "assistanceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "assistanceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseMilestone> milestones = new ArrayList<>();

    @OneToMany(mappedBy = "assistanceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseMessage> messages = new ArrayList<>();
}
