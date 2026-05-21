package com.trustplatform.volunteer;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.volunteer.dto.ApplyVolunteerRequest;
import com.trustplatform.volunteer.dto.VolunteerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    // 1. Submit Application
    @PostMapping("/apply") 
    @PreAuthorize("hasAnyRole('USER','VOLUNTEER','ADMIN')")
    public ResponseEntity<ApiSuccessResponse<VolunteerResponse>> apply(
            @Valid @RequestBody ApplyVolunteerRequest request) {

        VolunteerResponse response = volunteerService.applyForEvent(request);

        return ResponseEntity.ok(
                ApiSuccessResponse.<VolunteerResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Application submitted successfully")
                        .data(response)
                        .build()
        );
    }

    // 2. Fetch All Applications (For Admin Dashboard)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<VolunteerResponse> getAllApplications() {
        return volunteerService.getAllApplications();
    }

    // 3. Update Status (Generic PUT for Admin Dashboard)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateStatus(@PathVariable Long id, @RequestParam String status) {
        if ("approved".equalsIgnoreCase(status)) {
            volunteerService.approveVolunteer(id);
        } else if ("rejected".equalsIgnoreCase(status)) {
            volunteerService.rejectVolunteer(id);
        }
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public void approve(@PathVariable Long id) {
        volunteerService.approveVolunteer(id);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(@PathVariable Long id) {
        volunteerService.rejectVolunteer(id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER','VOLUNTEER','ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<VolunteerResponse>>> getMyApplications(
            org.springframework.security.core.Authentication authentication) {

        com.trustplatform.user.User user = (com.trustplatform.user.User) authentication.getPrincipal();
        List<VolunteerResponse> responses = volunteerService.getUserApplications(user.getId());

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<VolunteerResponse>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Volunteer applications fetched successfully")
                        .data(responses)
                        .build()
        );
    }
}