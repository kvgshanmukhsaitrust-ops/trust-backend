package com.trustplatform.applicant;

import com.trustplatform.applicant.dto.*;
import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.user.User;
import com.trustplatform.user.Permission;
import com.trustplatform.user.UserRepository;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Transactional
public class AssistanceCaseController {

    private final AssistanceCaseService caseService;
    private final UserRepository userRepository;

    // ==========================================
    // APPLICANT ENDPOINTS
    // ==========================================

    @PostMapping
    @PreAuthorize("hasAuthority('SUBMIT_ASSISTANCE')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> applyForAssistance(
            @Valid @RequestBody CaseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User applicant = getUserByEmail(userDetails.getUsername());
        AssistanceCase newCase = caseService.createCase(applicant, request);
        
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Application submitted successfully")
                        .data(mapToResponse(newCase))
                        .build()
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<Page<CaseResponse>>> getMyCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User applicant = getUserByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<CaseResponse> responses = caseService.getCasesForApplicant(applicant.getId(), pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<CaseResponse>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Personal cases fetched successfully")
                        .data(responses)
                        .build()
        );
    }

    @GetMapping("/{caseNumber}")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> getCaseDetails(
            @PathVariable String caseNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserByEmail(userDetails.getUsername());
        AssistanceCase c = caseService.getCaseByNumber(caseNumber);

        // Security check: Only applicant or administrative users (officers/admins) can view
        boolean isAdminOrOfficer = user.getRole().getPermissions().contains(Permission.MANAGE_APPLICATIONS);
        if (!isAdminOrOfficer && !c.getApplicant().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to view this case.");
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Case details fetched successfully")
                        .data(mapToResponse(c))
                        .build()
        );
    }

    // ==========================================
    // OFFICER & ADMIN ENDPOINTS
    // ==========================================

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<Page<CaseResponse>>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CaseStatus status) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CaseResponse> responses;
        if (status != null) {
            responses = caseService.getCasesByStatus(status, pageable).map(this::mapToResponse);
        } else {
            responses = caseService.getAllCases(pageable).map(this::mapToResponse);
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<CaseResponse>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("All cases fetched successfully")
                        .data(responses)
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/status")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateStatus(
            @PathVariable String caseNumber,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User actor = getUserByEmail(userDetails.getUsername());
        AssistanceCase updated = caseService.updateCaseStatus(caseNumber, request.getStatus(), request.getComment(), actor);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Case status updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/assign")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> assignCase(
            @PathVariable String caseNumber,
            @RequestParam Long officerId) {
        
        AssistanceCase updated = caseService.assignOfficer(caseNumber, officerId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Case officer assigned successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/notes")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateInternalNotes(
            @PathVariable String caseNumber,
            @RequestBody String notes) {
        
        AssistanceCase updated = caseService.updateInternalNotes(caseNumber, notes);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Internal notes updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/outcome")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateOutcome(
            @PathVariable String caseNumber,
            @RequestBody String outcome) {
        
        AssistanceCase updated = caseService.updateOutcomeDetails(caseNumber, outcome);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Outcome details updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @DeleteMapping("/{caseNumber}/documents/{documentId}")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteDoc(
            @PathVariable String caseNumber,
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserByEmail(userDetails.getUsername());
        caseService.deleteDocument(caseNumber, documentId, user);
        return ResponseEntity.ok(
                ApiSuccessResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Document deleted successfully")
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/schedule")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> scheduleVisit(
            @PathVariable String caseNumber,
            @RequestBody Map<String, String> body) {
        LocalDateTime visitTime = LocalDateTime.parse(body.get("visitTime"));
        AssistanceCase updated = caseService.scheduleVisit(caseNumber, visitTime);
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Visit scheduled successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/estimated-cost")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateEstimatedCost(
            @PathVariable String caseNumber,
            @RequestBody Map<String, java.math.BigDecimal> body) {
        AssistanceCase updated = caseService.updateEstimatedCost(caseNumber, body.get("cost"));
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Estimated cost updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/approved-amount")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateApprovedAmount(
            @PathVariable String caseNumber,
            @RequestBody Map<String, java.math.BigDecimal> body) {
        AssistanceCase updated = caseService.updateApprovedAmount(caseNumber, body.get("amount"));
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Approved amount updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/disbursed-amount")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateDisbursedAmount(
            @PathVariable String caseNumber,
            @RequestBody Map<String, java.math.BigDecimal> body) {
        AssistanceCase updated = caseService.updateDisbursedAmount(caseNumber, body.get("amount"));
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Disbursed amount updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/committee-notes")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateCommitteeNotes(
            @PathVariable String caseNumber,
            @RequestBody Map<String, String> body) {
        AssistanceCase updated = caseService.updateCommitteeNotes(caseNumber, body.get("notes"));
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Committee notes updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/tags")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> updateTags(
            @PathVariable String caseNumber,
            @RequestBody Map<String, String> body) {
        AssistanceCase updated = caseService.updateTags(caseNumber, body.get("tags"));
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Tags updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    @PutMapping("/{caseNumber}/escalate")
    @PreAuthorize("hasAuthority('MANAGE_APPLICATIONS')")
    public ResponseEntity<ApiSuccessResponse<CaseResponse>> toggleEscalation(
            @PathVariable String caseNumber,
            @RequestBody Map<String, Boolean> body) {
        AssistanceCase updated = caseService.toggleEscalation(caseNumber, Boolean.TRUE.equals(body.get("escalate")));
        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Escalation status updated successfully")
                        .data(mapToResponse(updated))
                        .build()
        );
    }

    // ==========================================
    // DOCUMENT UPLOAD ENDPOINTS
    // ==========================================

    @PostMapping("/{caseNumber}/documents")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<CaseDocumentResponse>> uploadDoc(
            @PathVariable String caseNumber,
            @RequestParam String docName,
            @RequestParam String docUrl,
            @RequestParam(required = false) String publicId,
            @RequestParam(required = false) String fileType,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserByEmail(userDetails.getUsername());
        AssistanceCase c = caseService.getCaseByNumber(caseNumber);

        // Security check: Only applicant or officers can upload documents
        boolean isAdminOrOfficer = user.getRole().getPermissions().contains(Permission.MANAGE_APPLICATIONS);
        if (!isAdminOrOfficer && !c.getApplicant().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to upload documents for this case.");
        }

        CaseDocument doc = caseService.uploadDocument(caseNumber, docName, docUrl, publicId, fileType);

        CaseDocumentResponse response = CaseDocumentResponse.builder()
                .id(doc.getId())
                .documentName(doc.getDocumentName())
                .documentUrl(doc.getDocumentUrl())
                .fileType(doc.getFileType())
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseDocumentResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Document attached successfully")
                        .data(response)
                        .build()
        );
    }

    // ==========================================
    // COMMUNICATION (MESSAGES) ENDPOINTS
    // ==========================================

    @PostMapping("/{caseNumber}/messages")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<CaseMessageResponse>> sendMessage(
            @PathVariable String caseNumber,
            @Valid @RequestBody CaseMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User sender = getUserByEmail(userDetails.getUsername());
        AssistanceCase c = caseService.getCaseByNumber(caseNumber);

        // Security check: Only applicant or administrative users can send messages
        boolean isAdminOrOfficer = sender.getRole().getPermissions().contains(Permission.MANAGE_APPLICATIONS);
        if (!isAdminOrOfficer && !c.getApplicant().getId().equals(sender.getId())) {
            throw new UnauthorizedException("You are not authorized to participate in this communication.");
        }

        // Applicants cannot send internal notes
        boolean isInternal = request.isInternal();
        if (!isAdminOrOfficer) {
            isInternal = false;
        }

        CaseMessage msg = caseService.postMessage(caseNumber, sender, request.getMessageContent(), isInternal);

        CaseMessageResponse response = CaseMessageResponse.builder()
                .id(msg.getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderRole(sender.getRole().name())
                .messageContent(msg.getMessageContent())
                .sentAt(msg.getSentAt())
                .isInternal(msg.isInternal())
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<CaseMessageResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Message posted successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{caseNumber}/messages")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<List<CaseMessageResponse>>> getMessages(
            @PathVariable String caseNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User viewer = getUserByEmail(userDetails.getUsername());
        List<CaseMessageResponse> responses = caseService.getMessagesForCase(caseNumber, viewer).stream()
                .map(m -> CaseMessageResponse.builder()
                        .id(m.getId())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getFullName())
                        .senderRole(m.getSender().getRole().name())
                        .messageContent(m.getMessageContent())
                        .sentAt(m.getSentAt())
                        .isInternal(m.isInternal())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<CaseMessageResponse>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Messages fetched successfully")
                        .data(responses)
                        .build()
        );
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private CaseResponse mapToResponse(AssistanceCase c) {
        return CaseResponse.builder()
                .id(c.getId())
                .caseNumber(c.getCaseNumber())
                .applicantId(c.getApplicant().getId())
                .applicantName(c.getApplicant().getFullName())
                .applicantEmail(c.getApplicant().getEmail())
                .category(c.getCategory())
                .status(c.getStatus())
                .priority(c.getPriority())
                .assignedOfficerId(c.getAssignedOfficer() != null ? c.getAssignedOfficer().getId() : null)
                .assignedOfficerName(c.getAssignedOfficer() != null ? c.getAssignedOfficer().getFullName() : null)
                .title(c.getTitle())
                .description(c.getDescription())
                .internalNotes(c.getInternalNotes())
                .outcomeDetails(c.getOutcomeDetails())
                .aiSummary(c.getAiSummary())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .documents(c.getDocuments().stream().map(d -> CaseDocumentResponse.builder()
                        .id(d.getId())
                        .documentName(d.getDocumentName())
                        .documentUrl(d.getDocumentUrl())
                        .fileType(d.getFileType())
                        .build()).collect(Collectors.toList()))
                .milestones(c.getMilestones().stream().map(m -> CaseMilestoneResponse.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .description(m.getDescription())
                        .statusSnapshot(m.getStatusSnapshot())
                        .timestamp(m.getTimestamp())
                        .build()).collect(Collectors.toList()))
                // Personal details
                .age(c.getAge())
                .gender(c.getGender())
                .dateOfBirth(c.getDateOfBirth())
                .mobile(c.getMobile())
                .email(c.getEmail())
                .aadhaar(c.getAadhaar())
                .pan(c.getPan())
                // Address
                .house(c.getHouse())
                .street(c.getStreet())
                .village(c.getVillage())
                .city(c.getCity())
                .district(c.getDistrict())
                .state(c.getState())
                .pincode(c.getPincode())
                // Family details
                .occupation(c.getOccupation())
                .monthlyIncome(c.getMonthlyIncome())
                .familyMembers(c.getFamilyMembers())
                .familyDependents(c.getFamilyDependents())
                // Tracking & Cost details
                .estimatedCost(c.getEstimatedCost())
                .preferredContact(c.getPreferredContact())
                .scheduledVisit(c.getScheduledVisit())
                .approvedAmount(c.getApprovedAmount())
                .disbursedAmount(c.getDisbursedAmount())
                .slaDeadline(c.getSlaDeadline())
                .escalated(c.getEscalated())
                .committeeNotes(c.getCommitteeNotes())
                .tags(c.getTags())
                .build();
    }
}
