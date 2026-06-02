package com.trustplatform.donation;

import com.trustplatform.donation.dto.CreateDonationRequest;
import com.trustplatform.donation.dto.DonationResponse;
import com.trustplatform.event.Event;
import com.trustplatform.event.EventRepository;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.user.User;
import com.trustplatform.notification.NotificationService;
import com.trustplatform.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.trustplatform.user.UserRepository userRepository;

    // =========================================
    // CREATE DONATION (anonymous)
    // =========================================
    @Transactional
    public DonationResponse createDonation(@Valid CreateDonationRequest request) {
        Donation donation = buildDonation(request, null);
        donation = donationRepository.save(donation);
        log.info("[DonationService] Created anonymous donation id={} amount={}", donation.getId(), donation.getAmount());
        return mapToResponse(donation);
    }

    // =========================================
    // CREATE DONATION (authenticated user)
    // =========================================
    @Transactional
    public Donation createDonation(User user, @Valid CreateDonationRequest request) {
        Donation donation = buildDonation(request, user);
        donation = donationRepository.save(donation);
        log.info("[DonationService] Created authenticated donation id={} for user={}", donation.getId(), user.getEmail());
        return donation;
    }

    private Donation buildDonation(CreateDonationRequest request, User user) {
        Donation donation = new Donation();
        donation.setAmount(request.getAmount());
        donation.setDonorName(request.getDonorName());
        donation.setDonorEmail(request.getDonorEmail());
        donation.setMessage(request.getMessage());
        donation.setStatus(DonationStatus.PENDING);
        donation.setReceiptUuid(UUID.randomUUID().toString());
        donation.setCorrelationId("TX-TRACER-" + UUID.randomUUID().toString());

        // PAN: normalize to uppercase, trim
        if (request.getDonorPan() != null && !request.getDonorPan().isBlank()) {
            donation.setDonorPan(request.getDonorPan().trim().toUpperCase());
        }
        if (request.getDonorAddress() != null && !request.getDonorAddress().isBlank()) {
            donation.setDonorAddress(request.getDonorAddress().trim());
        }

        if (user != null) donation.setUser(user);

        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            donation.setEvent(event);
        }
        return donation;
    }

    // =========================================
    // GET DONATIONS (admin paginated)
    // =========================================
    @Transactional(readOnly = true)
    public Page<DonationResponse> getDonations(DonationStatus status, Pageable pageable) {
        Page<Donation> donationPage = (status != null)
                ? donationRepository.findByStatus(status, pageable)
                : donationRepository.findAll(pageable);
        return donationPage.map(this::mapToResponse);
    }

    // =========================================
    // MARK SUCCESS (admin override)
    // =========================================
    @Transactional
    public synchronized void markDonationSuccess(Long id, String transactionId) {
        Donation donation = getAndValidateTransition(id, DonationStatus.SUCCESS);
        donation.setStatus(DonationStatus.SUCCESS);
        donation.setTransactionId(transactionId);
        donation.setReceiptNumber(generateReceiptNumber());
        donationRepository.save(donation);
        triggerSuccessNotifications(donation);
        log.info("[DonationService] Admin marked donation id={} as SUCCESS", id);
    }

    // =========================================
    // MARK FAILED (admin override)
    // =========================================
    @Transactional
    public synchronized void markDonationFailed(Long id) {
        Donation donation = getAndValidateTransition(id, DonationStatus.FAILED);
        donation.setStatus(DonationStatus.FAILED);
        donationRepository.save(donation);
        log.info("[DonationService] Admin marked donation id={} as FAILED", id);
    }

    // =========================================
    // STATE MACHINE VALIDATION
    // =========================================
    private Donation getAndValidateTransition(Long id, DonationStatus targetStatus) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found id=" + id));
        DonationStatus current = donation.getStatus();
        // Terminal states cannot be overwritten
        if (current == DonationStatus.REFUNDED) {
            throw new IllegalStateException("Donation id=" + id + " is already REFUNDED and cannot transition to " + targetStatus);
        }
        if (current == DonationStatus.SUCCESS && targetStatus == DonationStatus.PENDING) {
            throw new IllegalStateException("Cannot revert SUCCESS donation to PENDING");
        }
        if (current == DonationStatus.SUCCESS && targetStatus == DonationStatus.SUCCESS) {
            log.warn("[DonationService] Donation id={} is already SUCCESS - idempotent skip", id);
        }
        return donation;
    }

    // =========================================
    // USER DONATIONS
    // =========================================
    @Transactional(readOnly = true)
    public List<DonationResponse> getUserDonations(Long userId) {
        return donationRepository.findByUser_Id(userId)
                .stream().map(this::mapToResponse).toList();
    }

    public Donation getDonationById(Long donationId) {
        return donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found"));
    }

    // =========================================
    // PRIVATE HELPERS
    // =========================================
    private String generateReceiptNumber() {
        return "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void triggerSuccessNotifications(Donation donation) {
        try {
            notificationService.sendToAdmins("Donation Confirmed",
                    "₹" + donation.getAmount() + " from " + donation.getDonorName(), "DONATION");
            if (donation.getUser() != null) {
                notificationService.sendToUser(donation.getUser().getEmail(), "Donation Confirmed",
                        "Your ₹" + donation.getAmount() + " contribution receipt: " + donation.getReceiptNumber(), "DONATION");
            }
        } catch (Exception e) {
            log.error("[DonationService] Notification dispatch failed: {}", e.getMessage());
        }
    }

    public DonationResponse mapToResponse(Donation donation) {
        String maskedPan = null;
        boolean hasPan = false;
        if (donation.getDonorPan() != null && donation.getDonorPan().length() >= 4) {
            hasPan = true;
            String pan = donation.getDonorPan();
            maskedPan = "XXXXX" + pan.substring(5); // mask first 5 chars
            
            // STRICT PAN READ ACCESS AUDIT LOGGING
            try {
                String actor = "anonymous";
                if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                    actor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
                }
                auditService.log("PAN_DATA_ACCESS", actor, "Donation", 
                    "Read access to masked PAN card for donation record id=" + donation.getId() + " (correlationId=" + donation.getCorrelationId() + ")",
                    "127.0.0.1", "System-Telemetry", "SUCCESS", null);
            } catch (Exception e) {
                log.warn("Failed to record PAN access audit log: {}", e.getMessage());
            }
        }
        return DonationResponse.builder()
                .id(donation.getId())
                .amount(donation.getAmount())
                .donorName(donation.getDonorName())
                .donorEmail(donation.getDonorEmail())
                .message(donation.getMessage())
                .status(donation.getStatus())
                .receiptNumber(donation.getReceiptNumber())
                .receiptUuid(donation.getReceiptUuid())
                .receiptPdfPath(donation.getReceiptPdfPath())
                .transactionId(donation.getTransactionId())
                .paymentMethod(donation.getPaymentMethod())
                .refundDate(donation.getRefundDate())
                .refundReason(donation.getRefundReason())
                .createdAt(donation.getCreatedAt())
                .donorPanMasked(maskedPan)
                .hasPan(hasPan)
                .hasAddress(donation.getDonorAddress() != null && !donation.getDonorAddress().isBlank())
                .eventTitle(donation.getEvent() != null ? donation.getEvent().getTitle() : null)
                .correlationId(donation.getCorrelationId())
                .build();
    }

    @Transactional
    public synchronized void processVerifiedDonation(String payerEmail, java.math.BigDecimal amount, String transactionId, String gatewayOrderId, String metadata) {
        log.info("[DonationService] Processing verified donation from {}: amount={}, gatewayOrderId={}", payerEmail, amount, gatewayOrderId);
        
        Donation donation = null;
        if (gatewayOrderId != null) {
            donation = donationRepository.findByGatewayOrderId(gatewayOrderId).orElse(null);
        }

        if (donation == null) {
            donation = new Donation();
            donation.setAmount(amount);
            donation.setDonorName("Donor (" + payerEmail + ")");
            donation.setDonorEmail(payerEmail);
            donation.setStatus(DonationStatus.SUCCESS);
            donation.setTransactionId(transactionId);
            donation.setGatewayOrderId(gatewayOrderId);
            donation.setReceiptUuid(java.util.UUID.randomUUID().toString());
            donation.setReceiptNumber("REC-" + System.currentTimeMillis());
            donation.setCorrelationId("TX-WEBHOOK-" + java.util.UUID.randomUUID().toString());
            donation.setPaymentMethod("Razorpay Webhook");

            java.util.Optional<com.trustplatform.user.User> userOpt = userRepository.findByEmail(payerEmail);
            if (userOpt.isPresent()) {
                donation.setUser(userOpt.get());
                donation.setDonorName(userOpt.get().getFullName());
            }
        } else {
            if (donation.getStatus() != DonationStatus.SUCCESS) {
                donation.setStatus(DonationStatus.SUCCESS);
                donation.setTransactionId(transactionId);
                donation.setReceiptNumber("REC-" + System.currentTimeMillis());
                donation.setPaymentMethod("Razorpay Webhook");
            }
        }

        donationRepository.save(donation);

        // Automated donor role progression upgrade logic
        userRepository.findByEmail(payerEmail).ifPresent(user -> {
            if (user.getRole() == com.trustplatform.user.Role.USER) {
                user.setRole(com.trustplatform.user.Role.DONOR);
                userRepository.save(user);
                log.info("[DonationService] User {} successfully upgraded to DONOR", payerEmail);
            }
        });

        triggerSuccessNotifications(donation);
    }
}