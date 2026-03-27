package com.trustplatform.auth;

import com.trustplatform.exception.BadRequestException;
import com.trustplatform.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        return createInFamily(user, UUID.randomUUID().toString());
    }

    @Transactional
    public RefreshToken rotateRefreshToken(User user, String familyId) {
        return createInFamily(user, familyId);
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            log.error("SECURITY ALERT: Revoked token reuse detected. user={}, familyId={}",
                    refreshToken.getUser().getEmail(), refreshToken.getFamilyId());
            refreshTokenRepository.revokeAllByFamilyId(refreshToken.getFamilyId());
            throw new BadRequestException("Session invalidated. Please log in again.");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired. Please log in again.");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId);
        log.info("All refresh tokens revoked for userId={}", userId);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Expired refresh tokens cleaned up");
    }

    private RefreshToken createInFamily(User user, String familyId) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .familyId(familyId)
                .build();
        return refreshTokenRepository.save(token);
    }
}