package com.trustplatform.event.media;

import com.trustplatform.event.Event;
import jakarta.persistence.*;

@Entity
@Table(name = "event_media")
public class EventMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // ===== Constructors =====

    public EventMedia() {
    }

    public EventMedia(String mediaUrl, MediaType mediaType, Event event) {
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.event = event;
    }

    // ===== Getters =====

    public Long getId() {
        return id;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Event getEvent() {
        return event;
    }

    // ===== Setters =====

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}