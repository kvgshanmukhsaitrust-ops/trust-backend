package com.trustplatform.member;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trust_members")
@Getter @Setter 
@NoArgsConstructor @AllArgsConstructor @Builder
public class TrustMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String role; // e.g., "Founder", "President", "Secretary"

    private String tagline; // Short text for the front of the card

    @Column(length = 1000)
    private String bio; // Detailed text for the back of the card

    private String imageUrl; // For the profile avatar

    private String twitterUrl;
    private String linkedinUrl;

    private int displayOrder; // To control who appears first in the grid
}