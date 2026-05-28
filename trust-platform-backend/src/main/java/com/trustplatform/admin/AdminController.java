package com.trustplatform.admin;

import com.trustplatform.admin.dto.UpdateUserRoleRequest;
import com.trustplatform.common.api.ApiSuccessResponse;
import com.trustplatform.donation.Donation;
import com.trustplatform.user.User;
import com.trustplatform.volunteer.VolunteerApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.audit.AuditAction;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ===============================
    // UPDATE USER ROLE
    // ===============================
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @AuditAction("UPDATE_USER_ROLE")
    public ResponseEntity<ApiSuccessResponse<String>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody UpdateUserRoleRequest request) {

        adminService.updateUserRole(userId, request.getRole());

        return ResponseEntity.ok(
                ApiSuccessResponse.<String>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("User role updated successfully")
                        .data("Role updated to " + request.getRole())
                        .build()
        );
    }

    // ===============================
    // GET ALL USERS
    // ===============================
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<ApiSuccessResponse<List<User>>> getAllUsers() {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<User>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Users fetched successfully")
                        .data(adminService.getAllUsers())
                        .build()
        );
    }

    // ===============================
    // GET ALL VOLUNTEERS
    // ===============================
    @GetMapping("/volunteers")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS')")
    public ResponseEntity<ApiSuccessResponse<List<VolunteerApplication>>> getVolunteers() {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<VolunteerApplication>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Volunteers fetched successfully")
                        .data(adminService.getAllVolunteers())
                        .build()
        );
    }

    // ===============================
    // GET ALL DONATIONS
    // ===============================
    @GetMapping("/donations")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<ApiSuccessResponse<List<Donation>>> getDonations() {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<Donation>>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Donations fetched successfully")
                        .data(adminService.getAllDonations())
                        .build()
        );
    }
}