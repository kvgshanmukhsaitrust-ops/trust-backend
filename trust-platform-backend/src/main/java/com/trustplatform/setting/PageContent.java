package com.trustplatform.setting;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores named page sections as key/value text blocks.
 * e.g., key="HISTORY_INTRO", value="The trust was founded..."
 *       key="VISION_MISSION", value="To bring hope..."
 */
@Entity
@Table(name = "page_content")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PageContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contentKey;

    @Column(length = 10000)
    private String contentValue;
}
