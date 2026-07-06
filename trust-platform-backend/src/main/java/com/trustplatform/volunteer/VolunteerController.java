package com.trustplatform.volunteer;

import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.volunteer.dto.ApplyVolunteerRequest;
import com.trustplatform.volunteer.dto.VolunteerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.audit.AuditAction;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    // 1. Submit Application
    @PostMapping("/apply") 
    @PreAuthorize("hasAuthority('READ_CONTENT')")
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
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    public List<VolunteerResponse> getAllApplications() {
        return volunteerService.getAllApplications();
    }

    // 3. Update Status (Generic PUT for Admin Dashboard)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    @AuditAction("UPDATE_VOLUNTEER_STATUS")
    public void updateStatus(@PathVariable Long id, @RequestParam String status) {
        if ("approved".equalsIgnoreCase(status)) {
            volunteerService.approveVolunteer(id);
        } else if ("rejected".equalsIgnoreCase(status)) {
            volunteerService.rejectVolunteer(id);
        }
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    @AuditAction("APPROVE_VOLUNTEER")
    public void approve(@PathVariable Long id) {
        volunteerService.approveVolunteer(id);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    @AuditAction("REJECT_VOLUNTEER")
    public void reject(@PathVariable Long id) {
        volunteerService.rejectVolunteer(id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
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

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    @AuditAction("VOLUNTEER_CHECK_IN")
    public ResponseEntity<ApiSuccessResponse<VolunteerResponse>> checkIn(@PathVariable Long id) {
        VolunteerResponse response = volunteerService.checkIn(id);
        return ResponseEntity.ok(
                ApiSuccessResponse.<VolunteerResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Checked in successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    @AuditAction("VOLUNTEER_CHECK_OUT")
    public ResponseEntity<ApiSuccessResponse<VolunteerResponse>> checkOut(@PathVariable Long id) {
        VolunteerResponse response = volunteerService.checkOut(id);
        return ResponseEntity.ok(
                ApiSuccessResponse.<VolunteerResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Checked out successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    @AuditAction("ASSIGN_VOLUNTEER_ROLE")
    public ResponseEntity<ApiSuccessResponse<VolunteerResponse>> assignRole(
            @PathVariable Long id,
            @RequestParam String role) {
        VolunteerResponse response = volunteerService.assignRole(id, role);
        return ResponseEntity.ok(
                ApiSuccessResponse.<VolunteerResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Role assigned successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{id}/attendance")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    @AuditAction("VERIFY_VOLUNTEER_ATTENDANCE")
    public ResponseEntity<ApiSuccessResponse<VolunteerResponse>> verifyAttendance(
            @PathVariable Long id,
            @RequestParam(required = false) Double hours,
            @RequestParam Boolean verified) {
        VolunteerResponse response = volunteerService.verifyAttendance(id, hours, verified);
        return ResponseEntity.ok(
                ApiSuccessResponse.<VolunteerResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Attendance verified successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('READ_CONTENT')")
    public ResponseEntity<ApiSuccessResponse<com.trustplatform.volunteer.dto.VolunteerStatsResponse>> getStats(
            org.springframework.security.core.Authentication authentication) {
        com.trustplatform.user.User user = (com.trustplatform.user.User) authentication.getPrincipal();
        com.trustplatform.volunteer.dto.VolunteerStatsResponse response = volunteerService.getVolunteerStats(user.getId());
        return ResponseEntity.ok(
                ApiSuccessResponse.<com.trustplatform.volunteer.dto.VolunteerStatsResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Stats fetched successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiSuccessResponse<List<com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry>>> getLeaderboard() {
        List<com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry> response = volunteerService.getLeaderboard();
        return ResponseEntity.ok(
                ApiSuccessResponse.<List<com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Leaderboard fetched successfully")
                        .data(response)
                        .build()
        );
    }
}