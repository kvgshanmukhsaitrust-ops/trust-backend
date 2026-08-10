package com.trustplatform.admin;

import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationService;
import com.trustplatform.donation.dto.DonationResponse;
import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import com.trustplatform.volunteer.VolunteerApplication;
import com.trustplatform.volunteer.VolunteerRepository;
import com.trustplatform.volunteer.VolunteerService;
import com.trustplatform.volunteer.dto.VolunteerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VolunteerRepository volunteerRepository;
    private final DonationRepository donationRepository;
    private final DonationService donationService;
    private final VolunteerService volunteerService;

    // ===============================
    // UPDATE USER ROLE
    // ===============================
    public void updateUserRole(Long userId, Role role) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(role);

        userRepository.save(user);
    }

    // ===============================
    // GET ALL USERS
    // ===============================
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ===============================
    // GET ALL VOLUNTEER APPLICATIONS
    // ===============================
    @Transactional(readOnly = true)
    public List<VolunteerResponse> getAllVolunteers() {
        return volunteerRepository.findAll().stream()
                .map(volunteerService::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // ===============================
    // GET ALL DONATIONS
    // ===============================
    @Transactional(readOnly = true)
    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAll().stream()
                .map(donationService::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}