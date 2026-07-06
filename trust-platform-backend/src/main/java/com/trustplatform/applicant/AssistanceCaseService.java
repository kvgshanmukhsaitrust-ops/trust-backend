package com.trustplatform.applicant;

import com.trustplatform.ai.AiService;
import com.trustplatform.common.HtmlSanitizer;
import com.trustplatform.email.EmailService;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.notification.NotificationService;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistanceCaseService {

    private final AssistanceCaseRepository caseRepository;
    private final CaseDocumentRepository documentRepository;
    private final CaseMilestoneRepository milestoneRepository;
    private final CaseMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AiService aiService;

    @Transactional
    public AssistanceCase createCase(User applicant, com.trustplatform.applicant.dto.CaseRequest req) {
        String caseNumber = "CASE-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        CaseStatus status = Boolean.TRUE.equals(req.getDraft()) ? CaseStatus.DRAFT : CaseStatus.SUBMITTED;
        CasePriority priority = req.getPriority() != null ? req.getPriority() : CasePriority.MEDIUM;

        AssistanceCase newCase = AssistanceCase.builder()
                .caseNumber(caseNumber)
                .applicant(applicant)
                .category(req.getCategory())
                .status(status)
                .priority(priority)
                .title(req.getTitle())
                .description(req.getDescription() != null ? HtmlSanitizer.sanitize(req.getDescription()) : "")
                // Personal details
                .age(req.getAge())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .mobile(req.getMobile())
                .email(req.getEmail() != null ? req.getEmail() : applicant.getEmail())
                .aadhaar(req.getAadhaar())
                .pan(req.getPan())
                // Address
                .house(req.getHouse())
                .street(req.getStreet())
                .village(req.getVillage())
                .city(req.getCity())
                .district(req.getDistrict())
                .state(req.getState())
                .pincode(req.getPincode())
                // Family details
                .occupation(req.getOccupation())
                .monthlyIncome(req.getMonthlyIncome())
                .familyMembers(req.getFamilyMembers())
                .familyDependents(req.getFamilyDependents())
                // Requirement specifics
                .estimatedCost(req.getEstimatedCost())
                .preferredContact(req.getPreferredContact())
                .slaDeadline(LocalDateTime.now().plusDays(14)) // SLA standard 14 days
                .build();

        AssistanceCase savedCase = caseRepository.save(newCase);

        // Create initial milestone
        String milestoneTitle = status == CaseStatus.DRAFT ? "Draft Created" : "Case Submitted";
        String milestoneDesc = status == CaseStatus.DRAFT 
                ? "Your application draft has been saved."
                : "Your application for " + req.getCategory() + " assistance has been successfully submitted.";
        
        CaseMilestone milestone = CaseMilestone.builder()
                .assistanceCase(savedCase)
                .title(milestoneTitle)
                .description(milestoneDesc)
                .statusSnapshot(status)
                .timestamp(LocalDateTime.now())
                .build();
        milestoneRepository.save(milestone);

        // Only send alerts and generate AI if fully submitted
        if (status == CaseStatus.SUBMITTED) {
            // Notify Admins
            notificationService.sendToAdmins("New Assistance Case " + caseNumber, 
                    "A new case has been submitted by " + applicant.getFullName() + " under category " + req.getCategory(), "SYSTEM");

            // Send Email confirmation to Applicant
            try {
                emailService.sendEmail(applicant.getEmail(), "Assistance Application Received: " + caseNumber,
                        "Dear " + applicant.getFullName() + ",\n\nWe have received your application for " + req.getCategory() + " assistance. Your Case Number is " + caseNumber + ".\n\nYou can track the status on your dashboard.");
            } catch (Exception e) {
                log.error("Failed to send case confirmation email to {}", applicant.getEmail(), e);
            }

            // Generate AI Summary
            try {
                String aiSummary = aiService.summarizeCase(req.getTitle(), req.getDescription(), req.getCategory().name());
                savedCase.setAiSummary(aiSummary);
                caseRepository.save(savedCase);
            } catch (Exception e) {
                log.error("Failed to generate AI summary for case {}", caseNumber, e);
            }
        }

        return savedCase;
    }

    @Transactional(readOnly = true)
    public Page<AssistanceCase> getCasesForApplicant(Long applicantId, Pageable pageable) {
        return caseRepository.findByApplicantId(applicantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AssistanceCase> getAllCases(Pageable pageable) {
        return caseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AssistanceCase> getCasesByStatus(CaseStatus status, Pageable pageable) {
        return caseRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public AssistanceCase getCaseByNumber(String caseNumber) {
        return caseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Assistance case not found: " + caseNumber));
    }

    @Transactional
    public AssistanceCase updateCaseStatus(String caseNumber, CaseStatus newStatus, String comment, User actor) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        CaseStatus oldStatus = c.getStatus();
        
        if (oldStatus == newStatus) {
            return c;
        }

        c.setStatus(newStatus);
        
        // Save milestone
        CaseMilestone milestone = CaseMilestone.builder()
                .assistanceCase(c)
                .title("Status updated to " + newStatus)
                .description(comment)
                .statusSnapshot(newStatus)
                .timestamp(LocalDateTime.now())
                .build();
        milestoneRepository.save(milestone);

        // Log internal message/note about status change if comment is present
        if (comment != null && !comment.trim().isEmpty()) {
            CaseMessage changeMsg = CaseMessage.builder()
                    .assistanceCase(c)
                    .sender(actor)
                    .messageContent("Status changed from " + oldStatus + " to " + newStatus + ". Reason: " + comment)
                    .sentAt(LocalDateTime.now())
                    .isInternal(true)
                    .build();
            messageRepository.save(changeMsg);
        }

        AssistanceCase updated = caseRepository.save(c);

        // Notify applicant via WebSocket
        notificationService.sendToUser(c.getApplicant().getEmail(), "Case Update: " + caseNumber,
                "Your application status has been updated to " + newStatus, "SYSTEM");

        // Send Email alert
        try {
            emailService.sendEmail(c.getApplicant().getEmail(), "Case Update: " + caseNumber,
                    "Dear " + c.getApplicant().getFullName() + ",\n\nYour case status has been updated from " + oldStatus + " to " + newStatus + ".\n\nNotes: " + comment);
        } catch (Exception e) {
            log.error("Failed to send status update email", e);
        }

        return updated;
    }

    @Transactional
    public AssistanceCase assignOfficer(String caseNumber, Long officerId) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found: " + officerId));
        
        c.setAssignedOfficer(officer);
        c.setStatus(CaseStatus.UNDER_REVIEW);

        // Save milestone
        CaseMilestone milestone = CaseMilestone.builder()
                .assistanceCase(c)
                .title("Officer Assigned")
                .description("Your case has been assigned to Officer " + officer.getFullName() + " for active review.")
                .statusSnapshot(CaseStatus.UNDER_REVIEW)
                .timestamp(LocalDateTime.now())
                .build();
        milestoneRepository.save(milestone);

        AssistanceCase updated = caseRepository.save(c);

        // Notify Officer
        notificationService.sendToUser(officer.getEmail(), "New Assigned Case: " + caseNumber,
                "You have been assigned as the case officer for " + c.getApplicant().getFullName(), "SYSTEM");

        return updated;
    }

    @Transactional
    public CaseDocument uploadDocument(String caseNumber, String docName, String docUrl, String publicId, String fileType) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        
        CaseDocument doc = CaseDocument.builder()
                .assistanceCase(c)
                .documentName(docName)
                .documentUrl(docUrl)
                .publicId(publicId)
                .fileType(fileType)
                .build();
        
        return documentRepository.save(doc);
    }

    @Transactional
    public CaseMessage postMessage(String caseNumber, User sender, String content, boolean isInternal) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        
        CaseMessage msg = CaseMessage.builder()
                .assistanceCase(c)
                .sender(sender)
                .messageContent(HtmlSanitizer.sanitize(content))
                .sentAt(LocalDateTime.now())
                .isInternal(isInternal)
                .build();

        CaseMessage saved = messageRepository.save(msg);

        // If applicant posts, notify assigned officer (or admins if none assigned)
        if (sender.getId().equals(c.getApplicant().getId())) {
            String notifyEmail = c.getAssignedOfficer() != null ? c.getAssignedOfficer().getEmail() : "ADMIN";
            if ("ADMIN".equals(notifyEmail)) {
                notificationService.sendToAdmins("New Message for Case: " + caseNumber,
                        "Applicant " + sender.getFullName() + " sent a message: " + content, "SYSTEM");
            } else {
                notificationService.sendToUser(notifyEmail, "New Message for Case: " + caseNumber,
                        "Applicant " + sender.getFullName() + " sent a message: " + content, "SYSTEM");
            }
        } else {
            // If officer/admin posts, notify applicant (only if not internal note)
            if (!isInternal) {
                notificationService.sendToUser(c.getApplicant().getEmail(), "New message regarding case " + caseNumber,
                        sender.getFullName() + " sent a message: " + content, "SYSTEM");
            }
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CaseMessage> getMessagesForCase(String caseNumber, User viewer) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        
        // Applicants cannot see internal notes
        if (viewer.getId().equals(c.getApplicant().getId())) {
            return messageRepository.findByAssistanceCaseIdAndIsInternalFalseOrderBySentAtAsc(c.getId());
        }
        
        // Admins/officers see all messages
        return messageRepository.findByAssistanceCaseIdOrderBySentAtAsc(c.getId());
    }

    @Transactional(readOnly = true)
    public List<CaseMilestone> getMilestonesForCase(String caseNumber) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        return milestoneRepository.findByAssistanceCaseIdOrderByTimestampAsc(c.getId());
    }

    @Transactional
    public AssistanceCase updateInternalNotes(String caseNumber, String notes) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setInternalNotes(notes);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase updateOutcomeDetails(String caseNumber, String outcome) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setOutcomeDetails(outcome);
        return caseRepository.save(c);
    }

    @Transactional
    public void deleteDocument(String caseNumber, Long docId, User viewer) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        boolean isAdminOrOfficer = viewer.getRole().getPermissions().contains(com.trustplatform.user.Permission.MANAGE_APPLICATIONS);
        if (!isAdminOrOfficer && !c.getApplicant().getId().equals(viewer.getId())) {
            throw new com.trustplatform.exception.UnauthorizedException("You are not authorized to delete documents for this case.");
        }
        if (!isAdminOrOfficer && c.getStatus() != CaseStatus.DRAFT && c.getStatus() != CaseStatus.SUBMITTED) {
            throw new IllegalArgumentException("Documents can only be deleted during Draft or Submitted stages.");
        }
        CaseDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
        if (!doc.getAssistanceCase().getId().equals(c.getId())) {
            throw new IllegalArgumentException("Document does not belong to this case.");
        }
        documentRepository.delete(doc);
    }

    @Transactional
    public AssistanceCase scheduleVisit(String caseNumber, LocalDateTime visitTime) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setScheduledVisit(visitTime);
        // Save milestone
        CaseMilestone milestone = CaseMilestone.builder()
                .assistanceCase(c)
                .title("Field Visit Scheduled")
                .description("A field officer visit has been scheduled for " + visitTime.toString().replace("T", " ") + ".")
                .statusSnapshot(c.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
        milestoneRepository.save(milestone);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase updateEstimatedCost(String caseNumber, java.math.BigDecimal cost) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setEstimatedCost(cost);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase updateApprovedAmount(String caseNumber, java.math.BigDecimal amount) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setApprovedAmount(amount);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase updateDisbursedAmount(String caseNumber, java.math.BigDecimal amount) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setDisbursedAmount(amount);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase updateCommitteeNotes(String caseNumber, String notes) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setCommitteeNotes(notes);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase updateTags(String caseNumber, String tags) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setTags(tags);
        return caseRepository.save(c);
    }

    @Transactional
    public AssistanceCase toggleEscalation(String caseNumber, boolean escalate) {
        AssistanceCase c = getCaseByNumber(caseNumber);
        c.setEscalated(escalate);
        return caseRepository.save(c);
    }
}
