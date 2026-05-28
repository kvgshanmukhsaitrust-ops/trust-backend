package com.trustplatform.event.dto;

import com.trustplatform.event.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventSummaryResponse {
    private Long id;
    private String title;
    private String location;
    private String category;
    private LocalDateTime eventDate;
    private EventStatus status;
    private boolean published;
    private boolean featured;
    private String coverImageUrl;
    private String bannerUrl;
    private String subtitle;
}
