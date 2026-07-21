package com.trustplatform.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_versions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType; // e.g., "STORY", "EVENT", "PAGE_CONTENT"

    @Column(nullable = false)
    private String entityId; // string to handle both numeric IDs and string Keys (like PAGE_CONTENT keys)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contentSnapshot; // JSON representation or raw text value

    private int versionNumber;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private String createdBy; // username or user ID who made the change
}
