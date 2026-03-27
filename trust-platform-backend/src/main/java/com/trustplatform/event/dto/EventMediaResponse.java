package com.trustplatform.event.dto;

import com.trustplatform.event.media.MediaType;

public class EventMediaResponse {

    private String mediaUrl;
    private MediaType mediaType;

    public EventMediaResponse(String mediaUrl, MediaType mediaType) {
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}