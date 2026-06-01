package com.trustplatform.auth;

import com.trustplatform.email.EmailService;
import com.trustplatform.exception.BadRequestException;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.verification.expiration}")
    private Long verificationExpirationMs;

    public void sendVerificationEmail(User user) {

        String tokenValue = UUID.randomUUID().toString();

        VerificationToken token = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(Instant.now().plusMillis(verificationExpirationMs))
                .build();

        tokenRepository.save(token);

        String backendBaseUrl = "http://localhost:8080";
        try {
            backendBaseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromCurrentContextPath().build().toUriString();
        } catch (Exception e) {
            // Fallback in non-web thread contexts
        }

        String verificationLink =
                backendBaseUrl + "/api/auth/verify?token=" + tokenValue;

        System.out.println("==================================================");
        System.out.println("EMAIL VERIFICATION LINK GENERATED FOR " + user.getEmail() + ":");
        System.out.println(verificationLink);
        System.out.println("==================================================");

        String message =
                "Hi " + user.getFullName() + ",\n\n" +
                "Please verify your email by clicking the link below:\n\n" +
                verificationLink +
                "\n\nThis link expires in 24 hours.";

        try {
            emailService.sendEmailSync(
                    user.getEmail(),
                    "Verify Your Email - Trust Platform",
                    message
            );
        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
            throw new RuntimeException("Failed to send verification email to " + user.getEmail(), e);
        }
    }
    @Transactional
    public void verifyToken(String tokenValue) {

        VerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Verification token expired");
        }

        User user = token.getUser();
        user.setActive(true);        userRepository.save(user);

        // Delete token after successful verification
        tokenRepository.delete(token);
    }
}