package com.trustplatform.admin;

import com.trustplatform.audit.AuditLogRepository;
import com.trustplatform.auth.RefreshTokenRepository;
import com.trustplatform.auth.VerificationTokenRepository;
import com.trustplatform.auth.password.PasswordResetTokenRepository;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.payment.transaction.PaymentTransactionRepository;
import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import com.trustplatform.volunteer.VolunteerApplication;
import com.trustplatform.volunteer.VolunteerRepository;
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
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditLogRepository auditLogRepository;

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
    // DELETE USER (and all dependent records)
    // ===============================
    @Transactional
    public void deleteUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete payment transactions tied to this user's donations first,
        // since payment_transactions references donations via a FK.
        List<Donation> donations = donationRepository.findByUser_Id(userId);
        for (Donation donation : donations) {
            paymentTransactionRepository.deleteByDonation_Id(donation.getId());
        }

        // Delete donations made by this user
        donationRepository.deleteByUser_Id(userId);

        // Delete volunteer applications submitted by this user
        volunteerRepository.deleteByUserId(userId);

        // Delete auth-related tokens for this user
        verificationTokenRepository.deleteByUser_Id(userId);
        refreshTokenRepository.deleteByUser_Id(userId);
        passwordResetTokenRepository.deleteByUser_Id(userId);

        // Delete audit logs recorded against this user (matched by email, since
        // AuditLog stores the actor identifier rather than a user FK)
        auditLogRepository.deleteByPerformedBy(user.getEmail());

        // Finally, delete the user record itself
        userRepository.delete(user);
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