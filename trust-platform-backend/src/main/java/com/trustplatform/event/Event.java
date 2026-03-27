package com.trustplatform.event;

import com.trustplatform.common.BaseAuditableEntity;
import com.trustplatform.event.media.EventMedia;
import com.trustplatform.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
public class Event extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    private String location;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    private LocalDateTime registrationDeadline;

    private Integer maxVolunteers;

    private Integer currentVolunteerCount = 0;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private boolean deleted = false;
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventMedia> mediaList;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public Integer getCurrentVolunteerCount() {
		return currentVolunteerCount;
	}

	public void setCurrentVolunteerCount(Integer currentVolunteerCount) {
		this.currentVolunteerCount = currentVolunteerCount;
	}

	public EventStatus getStatus() {
		return status;
	}

	public void setStatus(EventStatus status) {
		this.status = status;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public List<EventMedia> getMediaList() {
		return mediaList;
	}

	public void setMediaList(List<EventMedia> mediaList) {
		this.mediaList = mediaList;
	}

    // getters & setters
    
}