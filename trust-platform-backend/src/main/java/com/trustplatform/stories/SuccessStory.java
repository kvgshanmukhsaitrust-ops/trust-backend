package com.trustplatform.stories;

import com.trustplatform.common.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "success_stories")
@Getter @Setter 
@NoArgsConstructor @AllArgsConstructor @Builder
public class SuccessStory extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000, nullable = false)
    private String description;

    private String imageUrl; // URL of the photo for the card

    private String category; // e.g., "Education", "Environment", "Health"
}