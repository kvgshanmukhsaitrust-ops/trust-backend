package com.trustplatform.event.dto;

import com.trustplatform.event.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private EventStatus status;
    private List<EventMediaResponse> media;

    public EventResponse(Long id,
                         String title,
                         String description,
                         String location,
                         LocalDateTime eventDate,
                         EventStatus status,
                         List<EventMediaResponse> media) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate;
        this.status = status;
        this.media = media;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDateTime getEventDate() { return eventDate; }
    public EventStatus getStatus() { return status; }
    public List<EventMediaResponse> getMedia() { return media; }
}