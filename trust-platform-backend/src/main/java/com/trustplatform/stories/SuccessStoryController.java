package com.trustplatform.stories;

import com.trustplatform.stories.dto.SuccessStoryResponse;
import com.trustplatform.media.MediaAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/success-stories")
@RequiredArgsConstructor
public class SuccessStoryController {

    private final SuccessStoryService storyService;

    // ── PUBLIC: only published stories (unless admin=true requested) ──
    @GetMapping
    public List<SuccessStoryResponse> getAllStories(
            @RequestParam(required = false) Boolean admin) {
        return storyService.getAllStories(admin);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessStoryResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(storyService.getStoryById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN: create ────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessStoryResponse> createStory(@RequestBody SuccessStory story) {
        return ResponseEntity.ok(storyService.createStory(story));
    }

    // ── ADMIN: update ────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessStoryResponse> updateStory(
            @PathVariable Long id,
            @RequestBody SuccessStory updated) {
        try {
            return ResponseEntity.ok(storyService.updateStory(id, updated));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN: delete ────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStory(@PathVariable Long id) {
        try {
            storyService.deleteStory(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN: toggle publish ────────────────────────────────────
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessStoryResponse> togglePublish(
            @PathVariable Long id, @RequestParam boolean value) {
        try {
            return ResponseEntity.ok(storyService.togglePublish(id, value));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN: update gallery ────────────────────────────────────
    @PutMapping("/{id}/gallery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateGallery(
            @PathVariable Long id,
            @RequestBody List<MediaAsset> gallery) {
        try {
            storyService.updateStoryGallery(id, gallery);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}