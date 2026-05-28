package com.trustplatform.notification;

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

    /**
     * Dispatch and persist notification to a specific user email asynchronously
     */
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
            Notification alert = Notification.builder()
                    .recipientEmail(email)
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

    /**
     * Dispatch and persist general admin alert notification asynchronously
     */
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

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(alert -> {
            alert.setRead(true);
            notificationRepository.save(alert);
        });
    }

    @Transactional
    public void markAllAsRead(String email) {
        List<Notification> unread = notificationRepository.findByRecipientEmailAndIsReadFalse(email);
        unread.forEach(alert -> alert.setRead(true));
        notificationRepository.saveAll(unread);
        
        // Also mark ROLE_ADMIN notifications if admin reads
        if (email.contains("admin") || email.equals("admin@trust.org")) {
            List<Notification> adminUnread = notificationRepository.findByRecipientEmailAndIsReadFalse("ROLE_ADMIN");
            adminUnread.forEach(alert -> alert.setRead(true));
            notificationRepository.saveAll(adminUnread);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (email.contains("admin") || email.equals("admin@trust.org")) {
            return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc("ROLE_ADMIN", pageable);
        }
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        if (email.contains("admin") || email.equals("admin@trust.org")) {
            return notificationRepository.countByRecipientEmailAndIsReadFalse("ROLE_ADMIN");
        }
        return notificationRepository.countByRecipientEmailAndIsReadFalse(email);
    }
}
