package com.trustplatform.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);
    
    long countByRecipientEmailAndIsReadFalse(String recipientEmail);
    
    List<Notification> findByRecipientEmailAndIsReadFalse(String recipientEmail);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n WHERE " +
            "((n.recipientEmail = :recipientEmail) OR (n.recipientRole = :recipientRole)) " +
            "AND (:category IS NULL OR n.category = :category) " +
            "AND (:isRead IS NULL OR n.isRead = :isRead) " +
            "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Notification> searchNotifications(
            @org.springframework.data.repository.query.Param("recipientEmail") String recipientEmail,
            @org.springframework.data.repository.query.Param("recipientRole") String recipientRole,
            @org.springframework.data.repository.query.Param("category") String category,
            @org.springframework.data.repository.query.Param("isRead") Boolean isRead,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(n) FROM Notification n WHERE " +
            "((n.recipientEmail = :recipientEmail) OR (n.recipientRole = :recipientRole)) " +
            "AND n.isRead = false")
    long countUnreadNotifications(
            @org.springframework.data.repository.query.Param("recipientEmail") String recipientEmail,
            @org.springframework.data.repository.query.Param("recipientRole") String recipientRole);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n WHERE " +
            "((n.recipientEmail = :recipientEmail) OR (n.recipientRole = :recipientRole)) " +
            "AND n.isRead = false")
    List<Notification> findUnreadNotifications(
            @org.springframework.data.repository.query.Param("recipientEmail") String recipientEmail,
            @org.springframework.data.repository.query.Param("recipientRole") String recipientRole);
}
