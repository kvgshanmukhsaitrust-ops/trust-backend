package com.trustplatform.donation;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.donation.dto.CreateDonationRequest;
import com.trustplatform.donation.dto.DonationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import com.trustplatform.user.User;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    // =========================================
    // CREATE DONATION (Logged-in users)
    // =========================================

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER','VOLUNTEER','ADMIN')")
    public ResponseEntity<ApiSuccessResponse<DonationResponse>> createDonation(
            @Valid @RequestBody CreateDonationRequest request) {

        DonationResponse response = donationService.createDonation(request);

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
    // GET DONATIONS (Admin only)
    // =========================================

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Page<DonationResponse>>> getDonations(
            @RequestParam(required = false) DonationStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        Page<DonationResponse> donations = donationService.getDonations(status, pageable);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<DonationResponse>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Donations fetched successfully")
                        .data(donations)
                        .build()
        );
    }

    // =========================================
    // MARK SUCCESS (Temporary - Admin)
    // =========================================

    @PatchMapping("/{id}/success")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<String>> markDonationSuccess(
            @PathVariable Long id,
            @RequestParam String transactionId) {

        donationService.markDonationSuccess(id, transactionId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Donation marked as SUCCESS")
                        .data("Success")
                        .build()
        );
    }

    // =========================================
    // MARK FAILED (Admin)
    // =========================================

    @PatchMapping("/{id}/failed")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<String>> markDonationFailed(
            @PathVariable Long id) {

        donationService.markDonationFailed(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Donation marked as FAILED")
                        .data("Failed")
                        .build()
        );
    }
    @GetMapping("/my")
    public ResponseEntity<List<Donation>> getMyDonations(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                donationService.getUserDonations(user.getId())
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<Donation> getDonation(@PathVariable Long id) {

        return ResponseEntity.ok(
                donationService.getDonationById(id)
        );
    }
}