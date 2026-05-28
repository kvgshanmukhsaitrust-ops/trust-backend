package com.trustplatform.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;      // e.g., "CREATE_EVENT", "DELETE_STORY"
    private String performedBy; // Email or User ID
    private String targetResource; // e.g. "Event", "SuccessStory", "User"
    
    @Column(columnDefinition = "TEXT")
    private String details;     // e.g., "Created event: Tuition point"
    
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    private String status;      // "SUCCESS", "FAILED"
    
    @Column(length = 2000)
    private String errorMessage;
    
    private LocalDateTime timestamp;
}