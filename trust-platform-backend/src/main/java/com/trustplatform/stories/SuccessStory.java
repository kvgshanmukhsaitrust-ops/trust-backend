package com.trustplatform.stories;

import com.trustplatform.common.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.validation.constraints.NotBlank;

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

    @NotBlank(message = "Story title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Story description is required")
    @Column(length = 2000, nullable = false)
    private String description;

    @Column(length = 2000)
    private String imageUrl;

    private String category;

    private boolean published = true;

    private boolean featured = false;

    private int displayOrder = 0;

    // --- New Enterprise Story fields ---
    private String location;

    @Column(length = 1000)
    private String subtitle;

    @Column(length = 2000)
    private String beforeImageUrl;

    @Column(length = 2000)
    private String afterImageUrl;

    @Column(length = 2000)
    private String videoUrl;

    @Column(length = 1000)
    private String testimonialQuote;
    private String testimonialAuthor;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private java.util.List<StoryTimelineMilestone> timeline = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private java.util.List<StoryImpactMetric> metrics = new java.util.ArrayList<>();

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean deleted = false;
}