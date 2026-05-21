package com.trustplatform.event;

import com.trustplatform.common.BaseAuditableEntity;
import com.trustplatform.event.media.EventMedia;
import com.trustplatform.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "events")
@SQLDelete(sql = "UPDATE events SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Event extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    private String location;

    private String category;

    @Column(length = 2000)
    private String bannerUrl;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    private LocalDateTime registrationDeadline;

    private Integer maxVolunteers;

    private Integer currentVolunteerCount = 0;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    private boolean published = true;

    private boolean featured = false;

    private int displayOrder = 0;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_highlights", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> highlights = new java.util.ArrayList<>();
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventMedia> mediaList;

    // --- New Enterprise Event fields ---
    @Column(length = 2000)
    private String coverImageUrl;

    @Column(length = 2000)
    private String heroImageUrl;

    @Column(length = 1000)
    private String subtitle;

    @Column(length = 2000)
    private String instagramUrl;

    @Column(length = 2000)
    private String youtubeUrl;

    @Column(length = 2000)
    private String facebookUrl;

    @Column(length = 2000)
    private String externalMediaUrl;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<EventFaq> faqs = new java.util.ArrayList<>();

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

    // getters & setters — new CMS fields
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<String> getHighlights() { return highlights; }
    public void setHighlights(List<String> highlights) { this.highlights = highlights; }

    // --- Getters & Setters for New Fields ---
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getExternalMediaUrl() { return externalMediaUrl; }
    public void setExternalMediaUrl(String externalMediaUrl) { this.externalMediaUrl = externalMediaUrl; }

    public List<EventFaq> getFaqs() { return faqs; }
    public void setFaqs(List<EventFaq> faqs) { this.faqs = faqs; }
}