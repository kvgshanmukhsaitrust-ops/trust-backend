package com.trustplatform.auth.password;

import com.trustplatform.auth.password.dto.ForgotPasswordRequest;
import com.trustplatform.auth.password.dto.ResetPasswordRequest;
import com.trustplatform.email.EmailService;
import com.trustplatform.email.EmailTemplateBuilder;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.exception.BadRequestException;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private String getRedirectBaseUrl() {
        if (frontendUrl != null && !frontendUrl.trim().isEmpty() && !"*".equals(frontendUrl.trim())) {
            String[] urls = frontendUrl.split(",");
            return urls[0].trim();
        }
        try {
            String backendUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromCurrentContextPath().build().toUriString();
            if (backendUrl != null && (backendUrl.contains("railway.app") || !backendUrl.contains("localhost"))) {
                return "https://trust-frontend-delta.vercel.app";
            }
        } catch (Exception e) {
            // Fallback in non-web contexts
        }
        return "http://localhost:5173";
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request, String origin) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        String frontendBaseUrl = (origin != null && !origin.trim().isEmpty()) ? origin : getRedirectBaseUrl();
        String resetLink =
                frontendBaseUrl + "/forgot-password?token=" + token;

        System.out.println("==================================================");
        System.out.println("PASSWORD RESET LINK GENERATED FOR " + user.getEmail() + ":");
        System.out.println(resetLink);
        System.out.println("==================================================");

        String emailBody =
                emailTemplateBuilder.buildPasswordResetEmail(
                        user.getFullName(),
                        resetLink
                );

        emailService.sendEmail(
                user.getEmail(),
                "Password Reset Request",
                emailBody
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid token"));

        if (token.isUsed()) {
            throw new BadRequestException("This password reset link has already been used. Please request a new one.");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset link has expired. Please request a new one.");
        }

        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setActive(true);

        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        try {
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now());
            String emailBody = emailTemplateBuilder.buildPasswordChangedEmail(user.getFullName(), dateStr);
            emailService.sendEmail(user.getEmail(), "Password Changed Successfully", emailBody);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PasswordResetService.class)
                .warn("Failed to send password changed confirmation email for {}", user.getEmail(), e);
        }
    }
}