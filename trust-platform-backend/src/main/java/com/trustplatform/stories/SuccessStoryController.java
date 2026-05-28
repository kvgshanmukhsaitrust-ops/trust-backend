package com.trustplatform.stories;

import com.trustplatform.stories.dto.SuccessStoryResponse;
import com.trustplatform.stories.dto.SuccessStorySummaryResponse;
import com.trustplatform.media.MediaAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.trustplatform.audit.AuditAction;

@RestController
@RequestMapping("/api/success-stories")
@RequiredArgsConstructor
public class SuccessStoryController {

    private final SuccessStoryService storyService;

    // ── PUBLIC: only published stories (unless admin=true requested) ──
    @GetMapping
    public List<SuccessStorySummaryResponse> getAllStories(
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
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("CREATE_STORY")
    public ResponseEntity<SuccessStoryResponse> createStory(@jakarta.validation.Valid @RequestBody SuccessStory story) {
        return ResponseEntity.ok(storyService.createStory(story));
    }

    // ── ADMIN: update ────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("UPDATE_STORY")
    public ResponseEntity<SuccessStoryResponse> updateStory(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody SuccessStory updated) {
        try {
            return ResponseEntity.ok(storyService.updateStory(id, updated));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN: delete ────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("DELETE_STORY")
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
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("PUBLISH_STORY")
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
    @PreAuthorize("hasAuthority('MANAGE_MEDIA')")
    @AuditAction("UPDATE_STORY_GALLERY")
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

    // ── ADMIN: reorder stories ───────────────────────────────────
    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("REORDER_STORIES")
    public ResponseEntity<Void> reorderStories(@RequestBody List<Long> storyIds) {
        try {
            storyService.reorderStories(storyIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── ADMIN: get versions ──────────────────────────────────────
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    public ResponseEntity<List<com.trustplatform.common.ContentVersion>> getStoryVersions(@PathVariable Long id) {
        return ResponseEntity.ok(storyService.getStoryVersions(id));
    }

    // ── ADMIN: rollback story ────────────────────────────────────
    @PostMapping("/{id}/rollback/{versionId}")
    @PreAuthorize("hasAuthority('MANAGE_STORIES')")
    @AuditAction("ROLLBACK_STORY")
    public ResponseEntity<SuccessStoryResponse> rollbackStory(@PathVariable Long id, @PathVariable Long versionId) {
        try {
            return ResponseEntity.ok(storyService.rollbackStory(id, versionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}