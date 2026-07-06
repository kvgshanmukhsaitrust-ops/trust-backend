package com.trustplatform.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail; // Either a specific user email, or "ROLE_ADMIN" for general admin notifications

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String category; // e.g. "ADMIN", "APPROVAL", "DONATION", "SYSTEM"

    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String correlationId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_role", length = 50)
    private String recipientRole;

    @Column(name = "notification_type", length = 100)
    private String notificationType;

    @Column(name = "target_entity", length = 100)
    private String targetEntity;

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "MEDIUM";
}
