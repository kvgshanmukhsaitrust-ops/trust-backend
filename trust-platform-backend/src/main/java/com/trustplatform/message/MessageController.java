package com.trustplatform.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.email.EmailService;
import com.trustplatform.email.EmailTemplateBuilder;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository repository;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ContactMessage> getMessages() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ContactMessage submitMessage(@RequestBody ContactMessage message) {
        ContactMessage saved = repository.save(message);

        // Send acknowledgement email asynchronously
        try {
            if (message.getEmail() != null && !message.getEmail().trim().isEmpty()) {
                String emailBody = emailTemplateBuilder.buildContactConfirmationEmail(
                        message.getName() != null ? message.getName() : "Visitor",
                        message.getMessage()
                );
                emailService.sendEmail(message.getEmail(), "Thank you for contacting us - KVGS Sai Trust", emailBody);
            }
        } catch (Exception e) {
            log.error("Failed to send contact form confirmation email to {}", message.getEmail(), e);
        }

        return saved;
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public void markAsRead(@PathVariable Long id) {
        ContactMessage msg = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        msg.setRead(true);
        repository.save(msg);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public long getUnreadCount() {
        return repository.countByIsReadFalse();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMessage(@PathVariable Long id) {
        repository.deleteById(id);
    }
}