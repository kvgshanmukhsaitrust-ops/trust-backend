package com.trustplatform.message;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository repository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ContactMessage> getMessages() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ContactMessage submitMessage(@RequestBody ContactMessage message) {
        return repository.save(message);
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