package com.trustplatform.stories;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "story_timeline_milestones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StoryTimelineMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private SuccessStory story;

    @Column(name = "milestone_date", nullable = false)
    private String date; // E.g., "Jan 2024" or a specific date string

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String imageUrl;

    @Column(name = "order_index")
    private int orderIndex = 0;
}
