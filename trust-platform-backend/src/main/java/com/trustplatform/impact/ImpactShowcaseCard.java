package com.trustplatform.impact;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "impact_showcase_cards")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ImpactShowcaseCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    private String icon;
    private String metricCount;
    private int displayOrder;

    private String subtitle;
    private String baseImage;
    private String revealImage;
    private String statLabel;
    private String tags; // Comma-separated list of tags
    private String accentColor;

    @Builder.Default
    private boolean deleted = false;
}
