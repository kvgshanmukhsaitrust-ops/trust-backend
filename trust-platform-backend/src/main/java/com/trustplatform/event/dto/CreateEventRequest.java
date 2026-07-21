package com.trustplatform.event.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventRequest {

    @NotBlank(message = "Initiative title is required")
    private String title;

    @NotBlank(message = "Initiative description is required")
    private String description;
    private String location;
    private String category;
    private String bannerUrl;
    @NotNull(message = "Initiative date is required")
    private LocalDateTime eventDate;
    private LocalDateTime registrationDeadline;
    private Integer maxVolunteers;
    @Builder.Default
    private Boolean published = true;
    @Builder.Default
    private Boolean featured = false;
    @Builder.Default
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