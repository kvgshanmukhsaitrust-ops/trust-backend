package com.trustplatform.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public String getFrontendUrl() {
        if (frontendUrl != null && !frontendUrl.trim().isEmpty() && !"*".equals(frontendUrl.trim())) {
            String[] urls = frontendUrl.split(",");
            return urls[0].trim();
        }
        return "http://localhost:5173";
    }

    private String buildTemplate(String name, String contentHtml, String ctaText, String ctaUrl) {
        String base = getFrontendUrl();
        String ctaHtml = "";
        if (ctaText != null && ctaUrl != null) {
            ctaHtml = """
                    <div class="email-cta-container">
                      <a href="%s" class="email-cta-button">%s</a>
                    </div>
                    """.formatted(ctaUrl, ctaText);
        }
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>KVGS Sai Charitable Trust</title>
                  <style>
                    body {
                      font-family: 'Inter', Helvetica, Arial, sans-serif;
                      background-color: #f7f9fc;
                      margin: 0;
                      padding: 0;
                      color: #333333;
                    }
                    .email-container {
                      max-width: 600px;
                      margin: 20px auto;
                      background: #ffffff;
                      border-radius: 12px;
                      overflow: hidden;
                      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
                      border: 1px solid #eef2f6;
                    }
                    .email-header {
                      background-color: #1a1a2e;
                      padding: 24px;
                      text-align: center;
                    }
                    .email-logo {
                      height: 60px;
                      width: 60px;
                      border-radius: 50%%;
                      display: block;
                      margin: 0 auto 10px auto;
                    }
                    .email-title {
                      color: #ffffff;
                      font-size: 20px;
                      font-weight: 700;
                      margin: 0;
                      letter-spacing: 0.5px;
                    }
                    .email-body {
                      padding: 30px 24px;
                      line-height: 1.6;
                      font-size: 15px;
                    }
                    .email-greeting {
                      font-size: 16px;
                      font-weight: 600;
                      color: #1a1a2e;
                      margin-top: 0;
                      margin-bottom: 16px;
                    }
                    .email-text {
                      color: #555555;
                      margin-bottom: 24px;
                    }
                    .email-cta-container {
                      text-align: center;
                      margin: 30px 0;
                    }
                    .email-cta-button {
                      background-color: #B07A3F;
                      color: #ffffff !important;
                      text-decoration: none;
                      padding: 12px 28px;
                      font-weight: 600;
                      border-radius: 8px;
                      display: inline-block;
                      box-shadow: 0 4px 10px rgba(176, 122, 63, 0.25);
                    }
                    .email-footer {
                      background-color: #f7f9fc;
                      padding: 24px;
                      text-align: center;
                      font-size: 12px;
                      color: #888888;
                      border-top: 1px solid #eef2f6;
                    }
                    .email-footer a {
                      color: #B07A3F;
                      text-decoration: none;
                    }
                    .email-divider {
                      border: 0;
                      border-top: 1px solid #eef2f6;
                      margin: 20px 0;
                    }
                  </style>
                </head>
                <body>
                  <div class="email-container">
                    <div class="email-header">
                      <img src="%s/logo.png" alt="KVGS Sai Logo" class="email-logo" />
                      <div class="email-title">KVGS Sai Charitable Trust</div>
                    </div>
                    <div class="email-body">
                      <div class="email-greeting">Hello %s,</div>
                      <div class="email-text">
                        %s
                      </div>
                      %s
                    </div>
                    <div class="email-footer">
                      <p>This email was sent by KVGS Sai Charitable Trust.</p>
                      <p>
                        <a href="%s">Visit Website</a> | 
                        <a href="mailto:kvgshanmukhsaitrust@gmail.com">Contact Us</a>
                      </p>
                      <p>&copy; 2026 KVGS Sai Charitable Trust. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(base, name, contentHtml, ctaHtml, base);
    }

    // ===============================
    // DONATION SUCCESS EMAIL
    // ===============================
    public String buildDonationSuccessEmail(String donorName, String receiptNumber, double amount, String date, String donationId) {
        String content = """
                <p>Thank you for your generous donation to KVGS Sai Charitable Trust. Your support makes a meaningful difference in our mission to empower lives.</p>
                <div style="background-color: #f7f9fc; border-left: 4px solid #B07A3F; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0;">
                  <strong style="color: #1a1a2e; display: block; margin-bottom: 8px;">Donation Details:</strong>
                  <table style="width: 100%%; font-size: 14px; border-collapse: collapse;">
                    <tr>
                      <td style="padding: 4px 0; color: #666666;">Donation ID:</td>
                      <td style="padding: 4px 0; font-weight: 600; text-align: right;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 4px 0; color: #666666;">Receipt Number:</td>
                      <td style="padding: 4px 0; font-weight: 600; text-align: right;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 4px 0; color: #666666;">Amount Received:</td>
                      <td style="padding: 4px 0; font-weight: 600; color: #B07A3F; text-align: right;">₹%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 4px 0; color: #666666;">Date:</td>
                      <td style="padding: 4px 0; font-weight: 600; text-align: right;">%s</td>
                    </tr>
                  </table>
                </div>
                <p>A printable PDF copy of your formal receipt is also attached to your account profile.</p>
                """.formatted(donationId, receiptNumber, String.format("%,.2f", amount), date);
        return buildTemplate(donorName, content, "View Account", getFrontendUrl() + "/dashboard");
    }

    // ===============================
    // VOLUNTEER APPROVAL EMAIL
    // ===============================
    public String buildVolunteerApprovalEmail(String volunteerName, String eventTitle) {
        String content = """
                <p>Congratulations! Your volunteer application to participate in the event <strong>"%s"</strong> has been approved.</p>
                <p>We look forward to your valuable participation. Our team will share further event details and schedule shortly.</p>
                <p>Thank you for stepping forward to make a difference in our community!</p>
                """.formatted(eventTitle);
        return buildTemplate(volunteerName, content, "View Dashboard", getFrontendUrl() + "/dashboard/volunteer");
    }

    // ===============================
    // VOLUNTEER REJECTION EMAIL
    // ===============================
    public String buildVolunteerRejectionEmail(String volunteerName, String eventTitle) {
        String content = """
                <p>Thank you for your interest in volunteering for the event <strong>"%s"</strong>.</p>
                <p>We received many high-quality applications and unfortunately, we are unable to accept your application at this time due to capacity limits.</p>
                <p>We sincerely appreciate your support and hope to have you join us for future initiatives.</p>
                """.formatted(eventTitle);
        return buildTemplate(volunteerName, content, "Explore Events", getFrontendUrl() + "/events");
    }

    // ===============================
    // PASSWORD RESET EMAIL
    // ===============================
    public String buildPasswordResetEmail(String name, String resetLink) {
        String content = """
                <p>We received a request to reset your password for your KVGS Sai Charitable Trust account.</p>
                <p>Click the button below to choose a new password. This link will expire in 30 minutes for security reasons.</p>
                <p>If you did not request this, you can safely ignore this email.</p>
                """;
        return buildTemplate(name, content, "Reset Password", resetLink);
    }

    // ===============================
    // WELCOME EMAIL
    // ===============================
    public String buildWelcomeEmail(String name) {
        String content = """
                <p>Welcome to KVGS Sai Charitable Trust!</p>
                <p>We are delighted to have you join our community. Our mission is to empower lives, support communities, and create lasting change through compassion, education, and service.</p>
                <p>Your account is now active. You can log in to your dashboard to explore upcoming initiatives, track your donations, or apply to volunteer.</p>
                """;
        return buildTemplate(name, content, "Go to Login", getFrontendUrl() + "/login");
    }

    // ===============================
    // GOOGLE WELCOME EMAIL
    // ===============================
    public String buildGoogleWelcomeEmail(String name) {
        String content = """
                <p>Welcome to KVGS Sai Charitable Trust!</p>
                <p>Thank you for registering an account using Google sign-in. We are excited to have you as part of our mission to create a positive impact through education, clean water, and community support.</p>
                <p>You can now sign in instantly at any time using your Google account to manage your profile and view updates.</p>
                """;
        return buildTemplate(name, content, "Go to Login", getFrontendUrl() + "/login");
    }

    // ===============================
    // PASSWORD CHANGED CONFIRMATION
    // ===============================
    public String buildPasswordChangedEmail(String name, String date) {
        String content = """
                <p>This is to confirm that the password for your KVGS Sai Charitable Trust account has been successfully changed.</p>
                <div style="background-color: #f7f9fc; border-left: 4px solid #B07A3F; padding: 12px; margin: 20px 0; border-radius: 0 8px 8px 0;">
                  <strong>Change Details:</strong><br>
                  Time: %s<br>
                  Status: Completed Successfully
                </div>
                <p><strong>Security Advice:</strong> If you did not authorize this change, please reset your password immediately using the Forgot Password flow, or contact our support team at <a href="mailto:kvgshanmukhsaitrust@gmail.com">kvgshanmukhsaitrust@gmail.com</a>.</p>
                """.formatted(date);
        return buildTemplate(name, content, "Secure Account", getFrontendUrl() + "/login");
    }

    // ===============================
    // CONTACT FORM CONFIRMATION
    // ===============================
    public String buildContactConfirmationEmail(String name, String originalMessage) {
        String content = """
                <p>We have received your message and will get back to you shortly.</p>
                <div style="background-color: #f7f9fc; border-left: 4px solid #B07A3F; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; font-size: 14px; font-style: italic;">
                  &ldquo;%s&rdquo;
                </div>
                <p>Thank you for reaching out to us. We appreciate your interest and support.</p>
                """.formatted(originalMessage);
        return buildTemplate(name, content, null, null);
    }

    // ===============================
    // CASE CREATED EMAIL
    // ===============================
    public String buildCaseCreatedEmail(String name, String caseNumber, String category) {
        String content = """
                <p>We have successfully received your application for <strong>%s assistance</strong>.</p>
                <div style="background-color: #f7f9fc; border-left: 4px solid #B07A3F; padding: 12px; margin: 20px 0; border-radius: 0 8px 8px 0;">
                  Case Number: <strong>%s</strong><br>
                  Category: <strong>%s</strong><br>
                  Status: <strong>PENDING REVIEW</strong>
                </div>
                <p>Our officers will review your case and supporting documents. You can track progress or submit messages directly on your dashboard.</p>
                """.formatted(category, caseNumber, category);
        return buildTemplate(name, content, "Track Status", getFrontendUrl() + "/dashboard/applicant");
    }

    // ===============================
    // CASE UPDATED EMAIL
    // ===============================
    public String buildCaseUpdatedEmail(String name, String caseNumber, String oldStatus, String newStatus, String comment) {
        String note = (comment != null && !comment.trim().isEmpty()) ? "<br>Notes: <em>" + comment + "</em>" : "";
        String content = """
                <p>Your assistance application status has been updated.</p>
                <div style="background-color: #f7f9fc; border-left: 4px solid #B07A3F; padding: 12px; margin: 20px 0; border-radius: 0 8px 8px 0;">
                  Case Number: <strong>%s</strong><br>
                  Status: <strong>%s</strong> (previously %s)%s
                </div>
                <p>Please log in to your dashboard to review any actions requested or view next steps.</p>
                """.formatted(caseNumber, newStatus, oldStatus, note);
        return buildTemplate(name, content, "View Case Dashboard", getFrontendUrl() + "/dashboard/applicant");
    }
}