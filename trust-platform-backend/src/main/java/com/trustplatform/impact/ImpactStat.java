package com.trustplatform.impact;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "impact_stats")
@Getter @Setter 
@NoArgsConstructor @AllArgsConstructor @Builder
public class ImpactStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category; // e.g., "WATER", "TREES", "STUDENTS", "CARRIAGES"

    @Column(nullable = false)
    private Long currentValue;

    private String unit;
    
    private String icon;

    @Builder.Default
    private boolean featured = false;

    @Builder.Default
    private int displayOrder = 0;
}