package com.trustplatform.event;

import com.trustplatform.event.dto.*;
import com.trustplatform.event.media.EventMedia;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.audit.AuditAction;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ── PUBLIC: paginated list of published events ──────────────
    @GetMapping
    public Page<EventSummaryResponse> getPublishedEvents(
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
    @PreAuthorize("hasAuthority('MANAGE_EVENTS')")
    @AuditAction("CREATE_EVENT")
    public ResponseEntity<EventResponse> createEvent(
            @jakarta.validation.Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.createEvent(request, authentication.getName()));
    }

    // ── ADMIN: update ────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_EVENTS')")
    @AuditAction("UPDATE_EVENT")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    // ── ADMIN: soft-delete ───────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_EVENTS')")
    @AuditAction("DELETE_EVENT")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/media")
    @PreAuthorize("hasAuthority('MANAGE_MEDIA')")
    @AuditAction("UPDATE_EVENT_MEDIA")
    public ResponseEntity<Void> updateEventMedia(@PathVariable Long id, @RequestBody List<EventMedia> mediaList) {
        eventService.updateEventMedia(id, mediaList);
        return ResponseEntity.ok().build();
    }

    // ── ADMIN: reorder events ───────────────────────────────────
    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('MANAGE_EVENTS')")
    @AuditAction("REORDER_EVENTS")
    public ResponseEntity<Void> reorderEvents(@RequestBody List<Long> eventIds) {
        try {
            eventService.reorderEvents(eventIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── ADMIN: publish/unpublish ─────────────────────────────────
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('MANAGE_EVENTS')")
    @AuditAction("PUBLISH_EVENT")
    public ResponseEntity<EventResponse> publish(
            @PathVariable Long id,
            @RequestParam boolean value) {
        return ResponseEntity.ok(eventService.togglePublish(id, value));
    }

    // ── ADMIN: feature/unfeature ─────────────────────────────────
    @PatchMapping("/{id}/feature")
    @PreAuthorize("hasAuthority('MANAGE_EVENTS')")
    @AuditAction("FEATURE_EVENT")
    public ResponseEntity<EventResponse> feature(
            @PathVariable Long id,
            @RequestParam boolean value) {
        return ResponseEntity.ok(eventService.toggleFeatured(id, value));
    }
}