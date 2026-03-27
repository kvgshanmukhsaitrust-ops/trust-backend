package com.trustplatform.event.dto;

import com.trustplatform.event.EventStatus;

import java.time.LocalDateTime;

public class UpdateEventRequest {

    private String title;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private LocalDateTime registrationDeadline;
    private Integer maxVolunteers;
    private EventStatus status;

    // getters & setters
}