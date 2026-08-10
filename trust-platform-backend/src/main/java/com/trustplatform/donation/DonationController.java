package com.trustplatform.donation;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.donation.dto.CreateDonationRequest;
import com.trustplatform.donation.dto.DonationResponse;
import com.trustplatform.payment.PdfReceiptService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.audit.AuditAction;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import com.trustplatform.user.User;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final PdfReceiptService pdfReceiptService;
    private final com.trustplatform.security.TurnstileService turnstileService;

    // =========================================
    // CREATE DONATION
    // =========================================
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<DonationResponse>> createDonation(
            @Valid @RequestBody CreateDonationRequest request,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest servletRequest) {

        String remoteIp = servletRequest.getHeader("X-Forwarded-For");
        if (remoteIp != null && !remoteIp.isBlank()) {
            remoteIp = remoteIp.split(",")[0].trim();
        } else {
            remoteIp = servletRequest.getRemoteAddr();
        }

        boolean isCaptchaValid = turnstileService.verifyToken(request.getTurnstileToken(), remoteIp);
        if (!isCaptchaValid) {
            throw new com.trustplatform.exception.BadRequestException("Invalid CAPTCHA / bot protection token. Please refresh and try again.");
        }

        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
        }

        DonationResponse response;
        if (user != null) {
            Donation donation = donationService.createDonation(user, request);
            response = donationService.mapToResponse(donation);
        } else {
            response = donationService.createDonation(request);
        }

        return ResponseEntity.status(201).body(
                ApiSuccessResponse.<DonationResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(201)
                        .message("Donation created successfully")
                        .data(response)
                        .build()
        );
    }

    // =========================================
    // GET DONATIONS (Admin)
    // =========================================
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<ApiSuccessResponse<Page<DonationResponse>>> getDonations(
            @RequestParam(required = false) DonationStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<DonationResponse>>builder()
                        .timestamp(LocalDateTime.now()).status(200)
                        .message("Donations fetched successfully")
                        .data(donationService.getDonations(status, pageable))
                        .build()
        );
    }

    // =========================================
    // MARK SUCCESS (Admin)
    // =========================================
    @PatchMapping("/{id}/success")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS')")
    @AuditAction("MARK_DONATION_SUCCESS")
    public ResponseEntity<ApiSuccessResponse<String>> markDonationSuccess(
            @PathVariable Long id, @RequestParam String transactionId) {
        donationService.markDonationSuccess(id, transactionId);
        return ResponseEntity.ok(ApiSuccessResponse.<String>builder()
                .timestamp(LocalDateTime.now()).status(200)
                .message("Donation marked as SUCCESS").data("Success").build());
    }

    // =========================================
    // MARK FAILED (Admin)
    // =========================================
    @PatchMapping("/{id}/failed")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS')")
    @AuditAction("MARK_DONATION_FAILED")
    public ResponseEntity<ApiSuccessResponse<String>> markDonationFailed(@PathVariable Long id) {
        donationService.markDonationFailed(id);
        return ResponseEntity.ok(ApiSuccessResponse.<String>builder()
                .timestamp(LocalDateTime.now()).status(200)
                .message("Donation marked as FAILED").data("Failed").build());
    }

    // =========================================
    // GET MY DONATIONS
    // =========================================
    @GetMapping("/my")
    public ResponseEntity<ApiSuccessResponse<List<DonationResponse>>> getMyDonations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                ApiSuccessResponse.<List<DonationResponse>>builder()
                        .timestamp(LocalDateTime.now()).status(200)
                        .message("Donations fetched successfully")
                        .data(donationService.getUserDonations(user.getId()))
                        .build()
        );
    }

    // =========================================
    // GET DONATION BY ID
    // =========================================
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable Long id, Authentication authentication) {
        Donation donation = donationService.getDonationById(id);
        validateDonationAccess(donation, authentication);
        return ResponseEntity.ok(donationService.mapToResponse(donation));
    }

    // =========================================
    // DOWNLOAD PDF RECEIPT
    // =========================================
    @GetMapping("/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> downloadReceipt(@PathVariable Long id, Authentication authentication) throws Exception {
        Donation donation = donationService.getDonationById(id);
        validateDonationAccess(donation, authentication);
        
        // Strict PAN download audit governance
        if (donation.getDonorPan() != null && !donation.getDonorPan().isBlank()) {
            try {
                String actor = authentication != null ? authentication.getName() : "anonymous";
                donationService.mapToResponse(donation); // maps and triggers standard PAN audit log natively!
            } catch (Exception e) {
                // fallthrough
            }
        }

        String pdfPath = pdfReceiptService.generateOrGetReceipt(id);
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(pdfFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + id + ".pdf\"")
                .body(resource);
    }

    // =========================================
    // DOWNLOAD PDF RECEIPT BY UUID (Guest Access)
    // =========================================
    @GetMapping("/receipt/uuid/{uuid}")
    public ResponseEntity<FileSystemResource> downloadReceiptByUuid(@PathVariable String uuid, Authentication authentication) throws Exception {
        Donation donation = donationService.getDonationByReceiptUuid(uuid);
        
        // Strict PAN download audit governance
        if (donation.getDonorPan() != null && !donation.getDonorPan().isBlank()) {
            try {
                String actor = authentication != null ? authentication.getName() : "anonymous";
                donationService.mapToResponse(donation); // maps and triggers standard PAN audit log natively!
            } catch (Exception e) {
                // fallthrough
            }
        }

        String pdfPath = pdfReceiptService.generateOrGetReceipt(donation.getId());
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(pdfFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + donation.getId() + ".pdf\"")
                .body(resource);
    }

    private void validateDonationAccess(Donation donation, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.trustplatform.exception.UnauthorizedException("Authentication required to access this donation");
        }
        
        // Admins and users with VIEW_ANALYTICS, MANAGE_SETTINGS, or MANAGE_APPLICATIONS permissions can access all donations
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) 
                        || "VIEW_ANALYTICS".equals(a.getAuthority())
                        || "MANAGE_SETTINGS".equals(a.getAuthority())
                        || "MANAGE_APPLICATIONS".equals(a.getAuthority()));
        
        if (isPrivileged) {
            return;
        }

        // If the donation is linked to a user, the current user's email must match the donation's user email
        if (donation.getUser() != null) {
            Object principal = authentication.getPrincipal();
            String currentUsername = null;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                currentUsername = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof com.trustplatform.user.User) {
                currentUsername = ((com.trustplatform.user.User) principal).getEmail();
            }
            if (currentUsername != null && donation.getUser().getEmail().equals(currentUsername)) {
                return;
            }
        }
        
        throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this donation");
    }
}
