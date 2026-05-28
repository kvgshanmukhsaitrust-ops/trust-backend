package com.trustplatform.event;

import com.trustplatform.event.dto.*;
import com.trustplatform.event.media.EventMedia;
import com.trustplatform.event.media.EventMediaRepository;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMediaRepository mediaRepository;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        EventMediaRepository mediaRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Event event = new Event();
        applyRequest(event, request);
        event.setStatus(EventStatus.UPCOMING);
        event.setCreatedBy(admin);
        eventRepository.save(event);
        return mapToResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, CreateEventRequest request) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        applyRequest(event, request);
        eventRepository.save(event);
        return mapToResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        event.setDeleted(true);
        eventRepository.save(event);
    }

    @Transactional
    public void updateEventMedia(Long id, List<EventMedia> mediaList) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

        event.getMediaList().clear();
        if (mediaList != null) {
            for (EventMedia media : mediaList) {
                media.setEvent(event);
                event.getMediaList().add(media);
            }
        }
        eventRepository.save(event);
    }

    @Transactional
    public void reorderEvents(List<Long> eventIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            Long id = eventIds.get(i);
            final int displayOrder = i;
            eventRepository.findById(id).ifPresent(event -> {
                event.setDisplayOrder(displayOrder);
                eventRepository.save(event);
            });
        }
    }

    @Transactional
    public EventResponse togglePublish(Long id, boolean publish) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        event.setPublished(publish);
        eventRepository.save(event);
        return mapToResponse(event);
    }

    @Transactional
    public EventResponse toggleFeatured(Long id, boolean featured) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        event.setFeatured(featured);
        eventRepository.save(event);
        return mapToResponse(event);
    }

    // Public: only published events
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> getPublishedEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("eventDate").descending()));
        return eventRepository.findByDeletedFalseAndPublishedTrue(pageable)
                .map(this::mapToSummaryResponse);
    }

    // Admin: all events including unpublished
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> getAllEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("eventDate").descending()));
        return eventRepository.findByDeletedFalse(pageable)
                .map(this::mapToSummaryResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapToResponse(event);
    }

    private void applyRequest(Event event, CreateEventRequest request) {
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getCategory() != null) event.setCategory(request.getCategory());
        if (request.getBannerUrl() != null) event.setBannerUrl(request.getBannerUrl());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getRegistrationDeadline() != null) event.setRegistrationDeadline(request.getRegistrationDeadline());
        if (request.getMaxVolunteers() != null) event.setMaxVolunteers(request.getMaxVolunteers());
        if (request.getPublished() != null) event.setPublished(request.getPublished());
        if (request.getFeatured() != null) event.setFeatured(request.getFeatured());
        if (request.getDisplayOrder() != null) event.setDisplayOrder(request.getDisplayOrder());
        if (request.getHighlights() != null) event.setHighlights(request.getHighlights());

        // New enterprise fields
        if (request.getCoverImageUrl() != null) event.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getHeroImageUrl() != null) event.setHeroImageUrl(request.getHeroImageUrl());
        if (request.getSubtitle() != null) event.setSubtitle(request.getSubtitle());
        if (request.getInstagramUrl() != null) event.setInstagramUrl(request.getInstagramUrl());
        if (request.getYoutubeUrl() != null) event.setYoutubeUrl(request.getYoutubeUrl());
        if (request.getFacebookUrl() != null) event.setFacebookUrl(request.getFacebookUrl());
        if (request.getExternalMediaUrl() != null) event.setExternalMediaUrl(request.getExternalMediaUrl());

        // Update FAQs using Cascade + orphan removal
        if (request.getFaqs() != null) {
            event.getFaqs().clear();
            for (CreateEventRequest.FaqRequest faqReq : request.getFaqs()) {
                EventFaq faq = EventFaq.builder()
                        .event(event)
                        .question(faqReq.getQuestion())
                        .answer(faqReq.getAnswer())
                        .displayOrder(faqReq.getDisplayOrder())
                        .build();
                event.getFaqs().add(faq);
            }
        }
    }

    public EventResponse mapToResponse(Event event) {
        EventStatus dynamicStatus = event.getStatus();
        if (event.getEventDate() != null && dynamicStatus != EventStatus.CANCELLED) {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate eventDay = event.getEventDate().toLocalDate();
            if (eventDay.isBefore(today)) {
                dynamicStatus = EventStatus.COMPLETED;
            } else if (eventDay.isEqual(today)) {
                dynamicStatus = EventStatus.ONGOING;
            } else {
                dynamicStatus = EventStatus.UPCOMING;
            }
        }

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .category(event.getCategory())
                .bannerUrl(event.getBannerUrl())
                .eventDate(event.getEventDate())
                .registrationDeadline(event.getRegistrationDeadline())
                .maxVolunteers(event.getMaxVolunteers())
                .currentVolunteerCount(event.getCurrentVolunteerCount())
                .status(dynamicStatus)
                .published(event.isPublished())
                .featured(event.isFeatured())
                .displayOrder(event.getDisplayOrder())
                .media(event.getMediaList() == null ? null :
                        event.getMediaList().stream()
                                .filter(m -> !m.isDeleted())
                                .map(m -> new EventMediaResponse(m.getMediaUrl(), m.getMediaType()))
                                .collect(Collectors.toList()))
                .highlights(event.getHighlights() == null ? null : new java.util.ArrayList<>(event.getHighlights()))
                .coverImageUrl(event.getCoverImageUrl())
                .heroImageUrl(event.getHeroImageUrl())
                .subtitle(event.getSubtitle())
                .instagramUrl(event.getInstagramUrl())
                .youtubeUrl(event.getYoutubeUrl())
                .facebookUrl(event.getFacebookUrl())
                .externalMediaUrl(event.getExternalMediaUrl())
                .faqs(event.getFaqs() == null ? null :
                        event.getFaqs().stream()
                                .map(f -> EventResponse.FaqResponse.builder()
                                        .id(f.getId())
                                        .question(f.getQuestion())
                                        .answer(f.getAnswer())
                                        .displayOrder(f.getDisplayOrder())
                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    public EventSummaryResponse mapToSummaryResponse(Event event) {
        EventStatus dynamicStatus = event.getStatus();
        if (event.getEventDate() != null && dynamicStatus != EventStatus.CANCELLED) {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate eventDay = event.getEventDate().toLocalDate();
            if (eventDay.isBefore(today)) {
                dynamicStatus = EventStatus.COMPLETED;
            } else if (eventDay.isEqual(today)) {
                dynamicStatus = EventStatus.ONGOING;
            } else {
                dynamicStatus = EventStatus.UPCOMING;
            }
        }

        return EventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .location(event.getLocation())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .status(dynamicStatus)
                .published(event.isPublished())
                .featured(event.isFeatured())
                .coverImageUrl(event.getCoverImageUrl())
                .bannerUrl(event.getBannerUrl())
                .subtitle(event.getSubtitle())
                .build();
    }
}
