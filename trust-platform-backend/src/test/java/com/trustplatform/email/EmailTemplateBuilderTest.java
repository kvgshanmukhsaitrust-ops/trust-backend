package com.trustplatform.email;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Renders every transactional email: no format errors, no unescaped user input, links on the real site. */
class EmailTemplateBuilderTest {

    private static final String EVIL = "<script>alert(1)</script><a href=\"https://evil.example\">click</a>";

    private EmailTemplateBuilder builder() {
        EmailTemplateBuilder b = new EmailTemplateBuilder();
        ReflectionTestUtils.setField(b, "frontendUrl", "https://shanmukasaitrust.org");
        return b;
    }

    @Test
    void everyTemplateRendersAndEscapesUserInput() {
        EmailTemplateBuilder b = builder();
        Map<String, String> all = new LinkedHashMap<>();
        all.put("donation", b.buildDonationSuccessEmail(EVIL, "REC-1", 8000.0, "2026-11-19 10:00:00", "42"));
        all.put("volApprove", b.buildVolunteerApprovalEmail(EVIL, EVIL));
        all.put("volReject", b.buildVolunteerRejectionEmail(EVIL, EVIL));
        all.put("reset", b.buildPasswordResetEmail(EVIL, "https://shanmukasaitrust.org/forgot-password?token=t"));
        all.put("welcome", b.buildWelcomeEmail(EVIL));
        all.put("googleWelcome", b.buildGoogleWelcomeEmail(EVIL));
        all.put("verify", b.buildVerificationEmail(EVIL, "https://api.shanmukasaitrust.org/api/auth/verify?token=t"));
        all.put("pwChanged", b.buildPasswordChangedEmail(EVIL, "2026-11-19 10:00:00"));
        all.put("contact", b.buildContactConfirmationEmail(EVIL, EVIL));
        all.put("caseCreated", b.buildCaseCreatedEmail(EVIL, "CASE-1", EVIL));
        all.put("caseUpdated", b.buildCaseUpdatedEmail(EVIL, "CASE-1", "SUBMITTED", "APPROVED", EVIL));

        all.forEach((name, html) -> {
            assertTrue(html.contains("<html>"), name + ": not an HTML document");
            assertFalse(html.contains("<script>"), name + ": user input was not escaped");
            assertFalse(html.contains("href=\"https://evil.example\""), name + ": injected link survived");
            assertFalse(html.contains("%s") || html.contains("%,"), name + ": unformatted placeholder left");
            assertFalse(html.contains("localhost"), name + ": link points at localhost");
        });
    }
}
