package com.trustplatform.event;

import com.trustplatform.event.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ── PUBLIC: paginated list of published events ──────────────
    @GetMapping
    public Page<EventResponse> getPublishedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean admin) {
        // Admin query param allows admin dashboard to see all (still JWT-gated by SecurityConfig)
        if (Boolean.TRUE.equals(admin)) {
            return eventService.getAllEvents(page, size);
        }
        return eventService.getPublishedEvents(page, size);
    }

    // ── PUBLIC: single event ─────────────────────────────────────
    @GetMapping("/{id}")
    public EventResponse getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    // ── ADMIN: create ────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @jakarta.validation.Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.createEvent(request, authentication.getName()));
    }

    // ── ADMIN: update ────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    // ── ADMIN: soft-delete ───────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // ── ADMIN: publish/unpublish ─────────────────────────────────
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> publish(
            @PathVariable Long id,
            @RequestParam boolean value) {
        return ResponseEntity.ok(eventService.togglePublish(id, value));
    }

    // ── ADMIN: feature/unfeature ─────────────────────────────────
    @PatchMapping("/{id}/feature")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> feature(
            @PathVariable Long id,
            @RequestParam boolean value) {
        return ResponseEntity.ok(eventService.toggleFeatured(id, value));
    }
}