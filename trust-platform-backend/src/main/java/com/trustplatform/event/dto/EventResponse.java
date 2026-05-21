package com.trustplatform.event.dto;

import com.trustplatform.event.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

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

    public EventResponse(Long id, String title, String description, String location,
                         String category, String bannerUrl,
                         LocalDateTime eventDate, LocalDateTime registrationDeadline,
                         Integer maxVolunteers, Integer currentVolunteerCount,
                         EventStatus status, boolean published, boolean featured,
                         int displayOrder, List<EventMediaResponse> media) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.bannerUrl = bannerUrl;
        this.eventDate = eventDate;
        this.registrationDeadline = registrationDeadline;
        this.maxVolunteers = maxVolunteers;
        this.currentVolunteerCount = currentVolunteerCount;
        this.status = status;
        this.published = published;
        this.featured = featured;
        this.displayOrder = displayOrder;
        this.media = media;
    }

    public EventResponse(Long id, String title, String description, String location,
                         String category, String bannerUrl,
                         LocalDateTime eventDate, LocalDateTime registrationDeadline,
                         Integer maxVolunteers, Integer currentVolunteerCount,
                         EventStatus status, boolean published, boolean featured,
                         int displayOrder, List<EventMediaResponse> media, List<String> highlights) {
        this(id, title, description, location, category, bannerUrl, eventDate, registrationDeadline,
             maxVolunteers, currentVolunteerCount, status, published, featured, displayOrder, media);
        this.highlights = highlights;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public String getBannerUrl() { return bannerUrl; }
    public LocalDateTime getEventDate() { return eventDate; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public Integer getMaxVolunteers() { return maxVolunteers; }
    public Integer getCurrentVolunteerCount() { return currentVolunteerCount; }
    public EventStatus getStatus() { return status; }
    public boolean isPublished() { return published; }
    public boolean isFeatured() { return featured; }
    public int getDisplayOrder() { return displayOrder; }
    public List<EventMediaResponse> getMedia() { return media; }
    public List<String> getHighlights() { return highlights; }
}