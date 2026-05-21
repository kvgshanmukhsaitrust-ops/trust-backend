package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin API for saving/editing page content sections (History, Vision, etc.)
 * PUT /api/admin/pages/{key}    — upsert a section value
 * GET /api/admin/pages/all      — get all page content
 * DELETE /api/admin/pages/{key} — delete a section
 */
@RestController
@RequestMapping("/api/admin/pages")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageContentController {

    private final PageContentRepository pageContentRepository;

    public AdminPageContentController(PageContentRepository pageContentRepository) {
        this.pageContentRepository = pageContentRepository;
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, String>> getAllPageContent() {
        Map<String, String> map = new HashMap<>();
        pageContentRepository.findAll().forEach(p -> map.put(p.getContentKey(), p.getContentValue()));
        return ResponseEntity.ok(map);
    }

    @PutMapping("/{key}")
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
        content.setContentValue(finalValue);
        pageContentRepository.save(content);
        return ResponseEntity.ok(finalValue);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deletePageContent(@PathVariable String key) {
        pageContentRepository.findByContentKey(key)
                .ifPresent(pageContentRepository::delete);
        return ResponseEntity.noContent().build();
    }
}
