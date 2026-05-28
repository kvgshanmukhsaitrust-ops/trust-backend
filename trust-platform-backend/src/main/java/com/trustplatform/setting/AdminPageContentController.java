package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.audit.AuditAction;

import com.trustplatform.common.ContentVersion;
import com.trustplatform.common.ContentVersionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API for saving/editing page content sections (History, Vision, etc.)
 * PUT /api/admin/pages/{key}    — upsert a section value
 * GET /api/admin/pages/all      — get all page content
 * DELETE /api/admin/pages/{key} — delete a section
 * GET /api/admin/pages/{key}/versions — get version history
 * POST /api/admin/pages/{key}/rollback/{versionId} — rollback
 */
@RestController
@RequestMapping("/api/admin/pages")
@PreAuthorize("hasAuthority('MANAGE_SETTINGS')")
public class AdminPageContentController {

    private final PageContentRepository pageContentRepository;
    private final ContentVersionRepository versionRepository;

    public AdminPageContentController(PageContentRepository pageContentRepository, ContentVersionRepository versionRepository) {
        this.pageContentRepository = pageContentRepository;
        this.versionRepository = versionRepository;
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, String>> getAllPageContent() {
        Map<String, String> map = new HashMap<>();
        pageContentRepository.findAll().forEach(p -> map.put(p.getContentKey(), p.getContentValue()));
        return ResponseEntity.ok(map);
    }

    @PutMapping("/{key}")
    @AuditAction("UPSERT_PAGE_CONTENT")
    public ResponseEntity<String> upsertPageContent(
            @PathVariable String key,
            @RequestBody(required = false) String value) {
        String cleaned = value == null ? "" : value.trim();
        // Strip surrounding quotes if sent as JSON string
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        final String finalValue = cleaned;
        
        PageContent content = pageContentRepository.findByContentKey(key)
                .orElseGet(() -> {
                    PageContent p = new PageContent();
                    p.setContentKey(key);
                    return p;
                });
        
        // Save version snapshot of old value if exists
        if (content.getId() != null && content.getContentValue() != null && !content.getContentValue().equals(finalValue)) {
            ContentVersion lastVersion = versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc("PAGE_CONTENT", key);
            int nextVersion = lastVersion == null ? 1 : lastVersion.getVersionNumber() + 1;
            
            ContentVersion cv = ContentVersion.builder()
                .entityType("PAGE_CONTENT")
                .entityId(key)
                .contentSnapshot(content.getContentValue())
                .versionNumber(nextVersion)
                .createdBy("ADMIN") // Could extract from SecurityContext
                .createdAt(java.time.LocalDateTime.now())
                .build();
            versionRepository.save(cv);
        }

        content.setContentValue(finalValue);
        pageContentRepository.save(content);
        return ResponseEntity.ok(finalValue);
    }

    @DeleteMapping("/{key}")
    @AuditAction("DELETE_PAGE_CONTENT")
    public ResponseEntity<Void> deletePageContent(@PathVariable String key) {
        pageContentRepository.findByContentKey(key)
                .ifPresent(pageContentRepository::delete);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{key}/versions")
    public ResponseEntity<List<ContentVersion>> getVersions(@PathVariable String key) {
        return ResponseEntity.ok(versionRepository.findByEntityTypeAndEntityIdOrderByVersionNumberDesc("PAGE_CONTENT", key));
    }

    @PostMapping("/{key}/rollback/{versionId}")
    @AuditAction("ROLLBACK_PAGE_CONTENT")
    public ResponseEntity<String> rollback(@PathVariable String key, @PathVariable Long versionId) {
        ContentVersion version = versionRepository.findById(versionId).orElseThrow();
        if (!"PAGE_CONTENT".equals(version.getEntityType()) || !key.equals(version.getEntityId())) {
            return ResponseEntity.badRequest().build();
        }
        return upsertPageContent(key, version.getContentSnapshot());
    }
}
