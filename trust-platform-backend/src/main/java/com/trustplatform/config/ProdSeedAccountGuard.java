package com.trustplatform.config;

import com.trustplatform.user.AuthProvider;
import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Production only. Earlier versions seeded demo accounts with well-known passwords (admin@trust.org / admin123 ...)
 * and reset them on every start, so those logins may exist in the production database.
 *
 * On every production start this guard:
 *  1. optionally creates / updates a real administrator from ADMIN_BOOTSTRAP_EMAIL + ADMIN_BOOTSTRAP_PASSWORD
 *     (password of 12+ characters), and
 *  2. deactivates any demo account whose password is still the well-known default. Default ADMIN accounts are
 *     only deactivated when a bootstrap administrator exists, so a deployment can never lock itself out.
 */
@Slf4j
@Profile("prod")
@Component
@Order(1)
@RequiredArgsConstructor
public class ProdSeedAccountGuard implements CommandLineRunner {

    private static final Map<String, String> KNOWN_DEFAULTS = Map.of(
            "admin@trust.org", "admin123",
            "admin2@trust.org", "admin123",
            "donor@trust.org", "donor123",
            "volunteer@trust.org", "volunteer123",
            "user@trust.org", "user123",
            "applicant@trust.org", "applicant123"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String bootstrapEmail;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        boolean bootstrapReady = false;
        if (bootstrapEmail != null && !bootstrapEmail.isBlank() && bootstrapPassword != null) {
            if (bootstrapPassword.length() < 12) {
                log.error("[ProdSeedAccountGuard] ADMIN_BOOTSTRAP_PASSWORD must be at least 12 characters: bootstrap admin NOT applied");
            } else {
                String email = bootstrapEmail.trim().toLowerCase();
                User admin = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                        .fullName("Administrator")
                        .email(email)
                        .authProvider(AuthProvider.LOCAL)
                        .build());
                admin.setPassword(passwordEncoder.encode(bootstrapPassword));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
                bootstrapReady = true;
                log.warn("[ProdSeedAccountGuard] Bootstrap administrator {} is active", email);
            }
        }

        for (Map.Entry<String, String> e : KNOWN_DEFAULTS.entrySet()) {
            User u = userRepository.findByEmail(e.getKey()).orElse(null);
            if (u == null || !u.isActive()) continue;
            if (u.getPassword() == null || !passwordEncoder.matches(e.getValue(), u.getPassword())) continue;

            if (u.getRole() == Role.ADMIN && !bootstrapReady) {
                log.error("[ProdSeedAccountGuard] SECURITY: admin account {} still uses its DEFAULT password and is publicly known. "
                        + "Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD (12+ chars) and redeploy: it will be deactivated.", e.getKey());
                continue;
            }
            u.setActive(false);
            userRepository.save(u);
            log.warn("[ProdSeedAccountGuard] Deactivated demo account {} (default password)", e.getKey());
        }
    }
}
