package com.trustplatform.event.dto;

import java.time.LocalDateTime;

public class CreateEventRequest {

    private String title;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private LocalDateTime registrationDeadline;
    private Integer maxVolunteers;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public LocalDateTime getEventDate() {
		return eventDate;
	}
	public void setEventDate(LocalDateTime eventDate) {
		this.eventDate = eventDate;
	}
	public LocalDateTime getRegistrationDeadline() {
		return registrationDeadline;
	}
	public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
		this.registrationDeadline = registrationDeadline;
	}
	public Integer getMaxVolunteers() {
		return maxVolunteers;
	}
	public void setMaxVolunteers(Integer maxVolunteers) {
		this.maxVolunteers = maxVolunteers;
	}

    // getters & setters
    
}