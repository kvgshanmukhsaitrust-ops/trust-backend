package com.trustplatform.applicant.dto;

import com.trustplatform.applicant.CaseCategory;
import com.trustplatform.applicant.CasePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CaseRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description details are required")
    private String description;

    @NotNull(message = "Category is required")
    private CaseCategory category;

    private CasePriority priority;

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

    // Additional Application specifics
    private BigDecimal estimatedCost;
    private String preferredContact;
    private Boolean draft;
}
