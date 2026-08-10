package com.trustplatform.auth;

import com.trustplatform.security.JwtService;
import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final com.trustplatform.email.EmailService emailService;
    private final com.trustplatform.email.EmailTemplateBuilder emailTemplateBuilder;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new com.trustplatform.exception.DuplicateResourceException("Email already registered");
        }
        String fullName = request.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = request.getEmail();
        }
        Role userRole = Role.USER;
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                Role parsedRole = Role.valueOf(request.getRole().toUpperCase());
                if (parsedRole == Role.ADMIN) {
                    throw new org.springframework.security.access.AccessDeniedException("Registration of administrator accounts is strictly forbidden.");
                }
                userRole = parsedRole;
            } catch (org.springframework.security.access.AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                userRole = Role.USER;
            }
        }

        User user = User.builder()
                .fullName(fullName)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .isActive(true) // local registration email verification turned off, auto-activated
                .authProvider(com.trustplatform.user.AuthProvider.LOCAL)
                .build();
        userRepository.save(user);
        log.info("New user registered and auto-activated: {}", request.getEmail());
        
        try {
            String emailBody = emailTemplateBuilder.buildWelcomeEmail(user.getFullName());
            emailService.sendEmail(user.getEmail(), "Welcome to KVGS Sai Charitable Trust", emailBody);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", user.getEmail(), e);
        }
        
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return buildResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public User oauthProvision(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .email(email)
                            .fullName(name != null ? name : email)
                            .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString())) // Secure BCrypt random password
                            .role(Role.USER)
                            .isActive(true) // OAuth users are pre-verified
                            .authProvider(com.trustplatform.user.AuthProvider.GOOGLE)
                            .build();
                    User saved = userRepository.save(user);
                    log.info("New OAuth user provisioned: {}", email);
                    try {
                        String emailBody = emailTemplateBuilder.buildGoogleWelcomeEmail(saved.getFullName());
                        emailService.sendEmail(saved.getEmail(), "Welcome to KVGS Sai Charitable Trust", emailBody);
                    } catch (Exception e) {
                        log.error("Failed to send Google OAuth welcome email to {}", email, e);
                    }
                    return saved;
                });
    }

    @Transactional
    public AuthenticationResponse login(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new com.trustplatform.exception.UnauthorizedException("Email not verified. Check your inbox.");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        log.info("User logged in: {}", user.getEmail());
        return buildResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenService
                .verifyRefreshToken(request.getRefreshToken());
        User user = existing.getUser();
        String familyId = existing.getFamilyId();

        refreshTokenService.revokeToken(existing);

        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefresh = refreshTokenService
                .rotateRefreshToken(user, familyId);

        return buildResponse(newAccessToken, newRefresh, user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        try {
            RefreshToken token = refreshTokenService
                    .verifyRefreshToken(refreshTokenValue);
            refreshTokenService.revokeToken(token);
        } catch (Exception e) {
            log.debug("Logout token revocation skipped: {}", e.getMessage());
        }
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
        log.info("All sessions revoked for userId={}", userId);
    }

    private AuthenticationResponse buildResponse(
            String accessToken, RefreshToken refreshToken, User user) {
        // refreshToken is @JsonIgnore in AuthenticationResponse — it will NOT appear in the JSON body.
        // AuthController extracts it here to set the HttpOnly cookie.
        return AuthenticationResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(AuthenticationResponse.UserDto.builder()
                        .name(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}