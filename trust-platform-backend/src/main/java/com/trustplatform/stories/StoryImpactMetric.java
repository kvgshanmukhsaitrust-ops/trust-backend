package com.trustplatform.stories;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "story_impact_metrics")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StoryImpactMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private SuccessStory story;

    @Column(nullable = false)
    private String label; // E.g., "Lives Impacted"

    @Column(nullable = false)
    private String value; // E.g., "500+"

    private String icon; // E.g., "Heart", "Users", etc.

    @Column(name = "display_order")
    @Builder.Default
    private int displayOrder = 0;
}
