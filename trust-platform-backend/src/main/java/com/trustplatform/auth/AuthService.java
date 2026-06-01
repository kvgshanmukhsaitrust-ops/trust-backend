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
                    if (userRepository.existsByRole(Role.ADMIN)) {
                        throw new com.trustplatform.exception.BadRequestException("An administrator account already exists. Registration of additional administrators is disabled.");
                    }
                }
                userRole = parsedRole;
            } catch (com.trustplatform.exception.BadRequestException e) {
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
                .isActive(true)
                .build();
        userRepository.save(user);
        // Email verification is intentionally skipped — users are active immediately on registration.
        // emailVerificationService.sendVerificationEmail(user);
        log.info("New user registered: {}", request.getEmail());
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return buildResponse(accessToken, refreshToken, user);
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