package com.trustplatform.notification;

import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // ── Internal role check ───────────────────────────────────────────────────
    private boolean isAdmin(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    @Async
    @Transactional
    public void sendToUser(String email, String title, String message, String category) {
        sendToUser(email, title, message, category, null);
    }

    @Async
    @Transactional
    public void sendToUser(String email, String title, String message, String category, String correlationId) {
        log.info("Dispatching async private notification to {}: [{}] {} (correlationId={})", email, category, title, correlationId);
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            String roleStr = user != null ? "ROLE_" + user.getRole().name() : null;
            Long userId = user != null ? user.getId() : null;

            Notification alert = Notification.builder()
                    .recipientEmail(email)
                    .recipientUserId(userId)
                    .recipientRole(roleStr)
                    .title(title)
                    .message(message)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .correlationId(correlationId)
                    .build();
            
            notificationRepository.save(alert);
            
            // Push via STOMP user queue
            messagingTemplate.convertAndSendToUser(email, "/queue/notifications", alert);
        } catch (Exception e) {
            log.error("Failed to dispatch real-time private notification to {}", email, e);
        }
    }

    @Async
    @Transactional
    public void sendToAdmins(String title, String message, String category) {
        sendToAdmins(title, message, category, null);
    }

    @Async
    @Transactional
    public void sendToAdmins(String title, String message, String category, String correlationId) {
        log.info("Dispatching async broadcast admin notification: [{}] {} (correlationId={})", category, title, correlationId);
        try {
            Notification alert = Notification.builder()
                    .recipientEmail("ROLE_ADMIN")
                    .recipientRole("ROLE_ADMIN")
                    .title(title)
                    .message(message)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .correlationId(correlationId)
                    .build();
            
            notificationRepository.save(alert);
            
            // Push via broadcast destination
            messagingTemplate.convertAndSend("/topic/admin-notifications", alert);
        } catch (Exception e) {
            log.error("Failed to dispatch real-time broadcast admin notification", e);
        }
    }

    @Async
    @Transactional
    public void sendNotification(Notification alert) {
        try {
            if (alert.getCreatedAt() == null) {
                alert.setCreatedAt(LocalDateTime.now());
            }
            if (alert.getRecipientEmail() != null && !"ROLE_ADMIN".equals(alert.getRecipientEmail())) {
                userRepository.findByEmail(alert.getRecipientEmail()).ifPresent(u -> {
                    alert.setRecipientUserId(u.getId());
                    if (alert.getRecipientRole() == null) {
                        alert.setRecipientRole("ROLE_" + u.getRole().name());
                    }
                });
            }
            
            notificationRepository.save(alert);
            
            if ("ROLE_ADMIN".equals(alert.getRecipientEmail()) || "ROLE_ADMIN".equals(alert.getRecipientRole())) {
                messagingTemplate.convertAndSend("/topic/admin-notifications", alert);
            } else if (alert.getRecipientEmail() != null) {
                messagingTemplate.convertAndSendToUser(alert.getRecipientEmail(), "/queue/notifications", alert);
            }
        } catch (Exception e) {
            log.error("Failed to send custom notification", e);
        }
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(alert -> {
            alert.setRead(true);
            alert.setReadAt(LocalDateTime.now());
            notificationRepository.save(alert);
        });
    }

    @Transactional
    public void markAllAsRead(String email) {
        boolean adminCheck = isAdmin(email);
        String targetEmail = adminCheck ? "ROLE_ADMIN" : email;
        String targetRole = adminCheck ? "ROLE_ADMIN" : userRepository.findByEmail(email)
                .map(u -> "ROLE_" + u.getRole().name())
                .orElse("ROLE_USER");

        List<Notification> unread = notificationRepository.findUnreadNotifications(targetEmail, targetRole);
        unread.forEach(alert -> {
            alert.setRead(true);
            alert.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(String email, String category, Boolean isRead, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        boolean adminCheck = isAdmin(email);
        String targetEmail = adminCheck ? "ROLE_ADMIN" : email;
        
        String targetRole = adminCheck ? "ROLE_ADMIN" : userRepository.findByEmail(email)
                .map(u -> "ROLE_" + u.getRole().name())
                .orElse("ROLE_USER");
        
        String cleanCategory = (category != null && !category.trim().isEmpty()) ? category : null;
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search : null;

        return notificationRepository.searchNotifications(targetEmail, targetRole, cleanCategory, isRead, cleanSearch, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        boolean adminCheck = isAdmin(email);
        String targetEmail = adminCheck ? "ROLE_ADMIN" : email;
        String targetRole = adminCheck ? "ROLE_ADMIN" : userRepository.findByEmail(email)
                .map(u -> "ROLE_" + u.getRole().name())
                .orElse("ROLE_USER");

        return notificationRepository.countUnreadNotifications(targetEmail, targetRole);
    }
}
