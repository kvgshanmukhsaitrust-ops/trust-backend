package com.trustplatform.user;

import com.trustplatform.donation.DonationService;
import com.trustplatform.donation.dto.DonationResponse;
import com.trustplatform.volunteer.VolunteerService;
import com.trustplatform.volunteer.dto.VolunteerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final DonationService donationService;
    private final VolunteerService volunteerService;

    // 1. User Count for Admin Dashboard stats
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public long getUserCount() {
        return userRepository.count();
    }

    // 2. Personal Activity Log (Donations + Events)
    @GetMapping("/me/activity")
    @PreAuthorize("hasAnyRole('USER', 'VOLUNTEER', 'APPLICANT')")
    public ResponseEntity<Map<String, Object>> getMyActivity(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> activityLog = new HashMap<>();
        
        activityLog.put("donations", donationService.getUserDonations(user.getId()));
        activityLog.put("eventApplications", volunteerService.getUserApplications(user.getId()));

        return ResponseEntity.ok(activityLog);
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String name = body.get("name");
        if (name != null && !name.trim().isEmpty()) {
            user.setFullName(name);
        }
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }
}
