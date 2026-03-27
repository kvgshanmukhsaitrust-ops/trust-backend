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

    private String action;      // e.g., "DONATION_RECEIVED", "VOLUNTEER_APPROVED"
    private String performedBy; // Email or User ID
    private String details;     // e.g., "Donation of ₹5000 by Rohith"
    private LocalDateTime timestamp;
}