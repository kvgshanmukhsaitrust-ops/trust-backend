package com.trustplatform.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String mailFrom;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.password}")
    private String mailPassword;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.port:587}")
    private int mailPort;

    @Async("mailExecutor")
    public CompletableFuture<Void> sendEmail(String to, String subject, String body) {
        try {
            sendEmailSync(to, subject, body);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public void sendEmailSync(String to, String subject, String body) throws Exception {
        // If password is a Brevo API key, bypass SMTP completely and send via HTTPS API (Port 443)
        // BUT if port is 2525, allow standard SMTP because port 2525 is whitelisted and not blocked by Railway!
        if (mailPassword != null && mailPassword.trim().startsWith("xsmtpsib-") && mailPort != 2525) {
            sendViaBrevoApi(to, subject, body);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);

            // Dynamically check if the body contains HTML tags to set contentType accordingly
            boolean isHtml = body.contains("<p>") || body.contains("<html>") || body.contains("<h2>") || body.contains("</a>");
            helper.setText(body, isHtml);

            mailSender.send(message);
            log.info("Email successfully sent synchronously via SMTP to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email synchronously via SMTP to: {}", to, e);
            throw e;
        }
    }

    private void sendViaBrevoApi(String to, String subject, String body) throws Exception {
        try {
            java.net.URL url = new java.net.URL("https://api.brevo.com/v3/smtp/email");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", mailPassword.trim());
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);

            boolean isHtml = body.contains("<p>") || body.contains("<html>") || body.contains("<h2>") || body.contains("</a>");

            // Construct JSON payload securely
            String escapedBody = escapeJson(body);
            String escapedSubject = escapeJson(subject);
            String escapedFrom = (mailFrom != null && mailFrom.contains("@")) ? mailFrom.trim() : "kvgshanmukhsaitrust@gmail.com";

            String jsonPayload = "{"
                + "\"sender\":{\"name\":\"Trust Platform\",\"email\":\"" + escapedFrom + "\"},"
                + "\"to\":[{\"email\":\"" + to.trim() + "\"}],"
                + "\"subject\":\"" + escapedSubject + "\","
                + (isHtml ? "\"htmlContent\":\"" + escapedBody + "\"" : "\"textContent\":\"" + escapedBody + "\"")
                + "}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                java.io.InputStream es = conn.getErrorStream();
                String errorMsg = "";
                if (es != null) {
                    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(es, "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        errorMsg = response.toString();
                    }
                }
                throw new RuntimeException("Brevo API returned HTTP " + code + ". Response: " + errorMsg);
            }
            log.info("Email successfully sent via Brevo HTTP API to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email synchronously via Brevo HTTP API to: {}", to, e);
            throw e;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}