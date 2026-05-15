package com.trustplatform.stories;

import com.trustplatform.stories.dto.SuccessStoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/success-stories")
@RequiredArgsConstructor
public class SuccessStoryController {

    private final SuccessStoryRepository repository;

    // ── PUBLIC: only published stories ──────────────────────────
    @GetMapping
    public List<SuccessStoryResponse> getAllStories(
            @RequestParam(required = false) Boolean admin) {
        List<SuccessStory> stories;
        if (Boolean.TRUE.equals(admin)) {
            stories = repository.findAll();
        } else {
            stories = repository.findByPublishedTrueOrderByDisplayOrderAscIdDesc();
        }
        return stories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessStoryResponse> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(s -> ResponseEntity.ok(toResponse(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── ADMIN: create ────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessStoryResponse> createStory(@RequestBody SuccessStory story) {
        return ResponseEntity.ok(toResponse(repository.save(story)));
    }

    // ── ADMIN: update ────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessStoryResponse> updateStory(
            @PathVariable Long id,
            @RequestBody SuccessStory updated) {
        return repository.findById(id).map(s -> {
            if (updated.getTitle() != null) s.setTitle(updated.getTitle());
            if (updated.getDescription() != null) s.setDescription(updated.getDescription());
            if (updated.getImageUrl() != null) s.setImageUrl(updated.getImageUrl());
            if (updated.getCategory() != null) s.setCategory(updated.getCategory());
            s.setPublished(updated.isPublished());
            s.setFeatured(updated.isFeatured());
            s.setDisplayOrder(updated.getDisplayOrder());
            return ResponseEntity.ok(toResponse(repository.save(s)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── ADMIN: delete ────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStory(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── ADMIN: toggle publish ────────────────────────────────────
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessStoryResponse> togglePublish(
            @PathVariable Long id, @RequestParam boolean value) {
        return repository.findById(id).map(s -> {
            s.setPublished(value);
            return ResponseEntity.ok(toResponse(repository.save(s)));
        }).orElse(ResponseEntity.notFound().build());
    }

    private SuccessStoryResponse toResponse(SuccessStory s) {
        return SuccessStoryResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .imageUrl(s.getImageUrl())
                .category(s.getCategory())
                .published(s.isPublished())
                .featured(s.isFeatured())
                .displayOrder(s.getDisplayOrder())
                .build();
    }
}