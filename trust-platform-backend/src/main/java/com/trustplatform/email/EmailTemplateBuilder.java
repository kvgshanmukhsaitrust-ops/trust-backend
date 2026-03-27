package com.trustplatform.email;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    // ===============================
    // DONATION SUCCESS EMAIL
    // ===============================
    public String buildDonationSuccessEmail(String donorName, String receiptNumber) {

        return """
                Dear %s,

                Thank you for your generous donation to our trust.

                Your Receipt Number: %s

                Your contribution helps us continue our mission and make a meaningful impact.

                If you have any questions, feel free to contact us.

                Regards,
                Trust Management Team
                """.formatted(donorName, receiptNumber);
    }

    // ===============================
    // VOLUNTEER APPROVAL EMAIL
    // ===============================
    public String buildVolunteerApprovalEmail(String volunteerName, String eventTitle) {

        return """
                Dear %s,

                Congratulations!

                Your volunteer application for the event "%s" has been approved.

                We look forward to your valuable participation.

                Further event details will be shared soon.

                Regards,
                Trust Management Team
                """.formatted(volunteerName, eventTitle);
    }

    // ===============================
    // VOLUNTEER REJECTION EMAIL
    // ===============================
    public String buildVolunteerRejectionEmail(String volunteerName, String eventTitle) {

        return """
                Dear %s,

                Thank you for applying to volunteer for the event "%s".

                Unfortunately, we are unable to approve your application at this time.

                We sincerely appreciate your interest and hope to see you in future initiatives.

                Regards,
                Trust Management Team
                """.formatted(volunteerName, eventTitle);
    }
    public String buildPasswordResetEmail(String name, String resetLink) {

        return """
            <h2>Password Reset Request</h2>
            <p>Hello %s,</p>
            <p>You requested a password reset.</p>
            <p>Click the link below to reset your password:</p>
            <a href="%s">Reset Password</a>
            <p>This link expires in 30 minutes.</p>
            """.formatted(name, resetLink);
    }
}