package com.trustplatform.event;

import com.trustplatform.event.dto.*;
import com.trustplatform.event.media.EventMedia;
import com.trustplatform.event.media.MediaType;
import com.trustplatform.event.media.EventMediaRepository;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;



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

        if (request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date must be in future");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setMaxVolunteers(request.getMaxVolunteers());
        event.setStatus(EventStatus.UPCOMING);
        event.setCreatedBy(admin);

        eventRepository.save(event);

        return mapToResponse(event);
    }

    public Page<EventResponse> getAllEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        return eventRepository.findByDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapToResponse(event);
    }

    private EventResponse mapToResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getEventDate(),
                event.getStatus(),
                event.getMediaList() == null ? null :
                        event.getMediaList().stream()
                                .filter(m -> !m.isDeleted())
                                .map(m -> new EventMediaResponse(
                                        m.getMediaUrl(),
                                        m.getMediaType()   // ✅ CORRECT
                                ))
                                .collect(Collectors.toList())
        );
    }    
    }
