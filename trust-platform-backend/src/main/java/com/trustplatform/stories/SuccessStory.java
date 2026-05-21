package com.trustplatform.stories;

import com.trustplatform.common.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "success_stories")
@SQLDelete(sql = "UPDATE success_stories SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
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

    private String imageUrl;

    private String category;

    private boolean published = true;

    private boolean featured = false;

    private int displayOrder = 0;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean deleted = false;
}