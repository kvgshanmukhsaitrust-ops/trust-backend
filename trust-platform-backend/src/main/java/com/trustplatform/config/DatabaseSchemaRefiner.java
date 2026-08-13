package com.trustplatform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaRefiner {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void refineSchema() {
        try {
            log.info("[DatabaseSchemaRefiner] Altering donations.status to VARCHAR(50) to support new payment states...");
            jdbcTemplate.execute("ALTER TABLE donations MODIFY COLUMN status VARCHAR(50) NOT NULL;");
            log.info("[DatabaseSchemaRefiner] Alter on donations.status successfully executed!");
        } catch (Exception e) {
            log.warn("[DatabaseSchemaRefiner] Failed to execute status alter query: {}.", e.getMessage());
        }

        try {
            log.info("[DatabaseSchemaRefiner] Altering users.role to VARCHAR(50) to support DONOR role...");
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(50) NOT NULL;");
            log.info("[DatabaseSchemaRefiner] Alter on users.role successfully executed!");
        } catch (Exception e) {
            log.warn("[DatabaseSchemaRefiner] Failed to execute role alter query: {}.", e.getMessage());
        }

        try {
            log.info("[DatabaseSchemaRefiner] Altering donations.donor_pan to VARCHAR(255) to support encrypted values...");
            jdbcTemplate.execute("ALTER TABLE donations MODIFY COLUMN donor_pan VARCHAR(255);");
            log.info("[DatabaseSchemaRefiner] Alter on donations.donor_pan successfully executed!");
        } catch (Exception e) {
            log.warn("[DatabaseSchemaRefiner] Failed to execute donor_pan alter query: {}.", e.getMessage());
        }

        try {
            log.info("[DatabaseSchemaRefiner] Altering impact_showcase_cards.base_image and reveal_image to VARCHAR(1000) to support long Cloudinary paths...");
            jdbcTemplate.execute("ALTER TABLE impact_showcase_cards MODIFY COLUMN base_image VARCHAR(1000);");
            jdbcTemplate.execute("ALTER TABLE impact_showcase_cards MODIFY COLUMN reveal_image VARCHAR(1000);");
            log.info("[DatabaseSchemaRefiner] Alter on base_image and reveal_image successfully executed!");
        } catch (Exception e) {
            log.warn("[DatabaseSchemaRefiner] Failed to execute image column alter query: {}.", e.getMessage());
        }
    }
}
