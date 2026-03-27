package com.trustplatform.admin;

import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import com.trustplatform.volunteer.VolunteerApplication;
import com.trustplatform.volunteer.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VolunteerRepository volunteerRepository;
    private final DonationRepository donationRepository;

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
    public List<VolunteerApplication> getAllVolunteers() {
        return volunteerRepository.findAll();
    }

    // ===============================
    // GET ALL DONATIONS
    // ===============================
    public List<Donation> getAllDonations() {
        return donationRepository.findAll();
    }
}