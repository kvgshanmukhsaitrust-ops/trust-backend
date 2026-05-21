package com.trustplatform.event.dto;

import com.trustplatform.event.EventStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private String category;
    private String bannerUrl;
    private LocalDateTime eventDate;
    private LocalDateTime registrationDeadline;
    private Integer maxVolunteers;
    private Integer currentVolunteerCount;
    private EventStatus status;
    private boolean published;
    private boolean featured;
    private int displayOrder;
    private List<EventMediaResponse> media;
    private List<String> highlights;

    // --- New Enterprise Fields ---
    private String coverImageUrl;
    private String heroImageUrl;
    private String subtitle;
    private String instagramUrl;
    private String youtubeUrl;
    private String facebookUrl;
    private String externalMediaUrl;
    private List<FaqResponse> faqs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaqResponse {
        private Long id;
        private String question;
        private String answer;
        private int displayOrder;
    }
}