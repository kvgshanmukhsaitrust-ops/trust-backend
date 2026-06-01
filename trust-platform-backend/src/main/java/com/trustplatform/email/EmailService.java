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

    @Async("mailExecutor")
    public CompletableFuture<Void> sendEmail(String to, String subject, String body) {
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
            log.info("Email successfully sent to: {}", to);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}