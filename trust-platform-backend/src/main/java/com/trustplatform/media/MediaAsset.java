package com.trustplatform.media;

import com.trustplatform.event.media.MediaType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_assets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_type", nullable = false)
    private String ownerType; // e.g., "STORY", "EVENT"

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 1000)
    private String thumbnailUrl;

    private String caption;

    @Column(name = "order_index")
    private int orderIndex = 0;

    @Column(name = "public_id")
    private String publicId;

    private Integer width;
    private Integer height;

    @Column(name = "aspect_ratio")
    private Double aspectRatio;

    private Double duration;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
