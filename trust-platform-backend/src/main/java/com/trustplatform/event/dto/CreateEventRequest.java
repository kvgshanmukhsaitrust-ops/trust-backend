package com.trustplatform.event.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventRequest {

    private String title;
    private String description;
    private String location;
    private String category;
    private String bannerUrl;
    private LocalDateTime eventDate;
    private LocalDateTime registrationDeadline;
    private Integer maxVolunteers;
    private Boolean published = true;
    private Boolean featured = false;
    private Integer displayOrder = 0;
    private List<String> highlights;

    // --- New Enterprise fields ---
    private String coverImageUrl;
    private String heroImageUrl;
    private String subtitle;
    private String instagramUrl;
    private String youtubeUrl;
    private String facebookUrl;
    private String externalMediaUrl;
    private List<FaqRequest> faqs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaqRequest {
        private String question;
        private String answer;
        private int displayOrder;
    }
}