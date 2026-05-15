package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Public read-only API for page content sections (History, Vision, etc.)
 * GET /api/public/pages/all   — returns all page content as key/value map
 * GET /api/public/pages/{key} — returns a single section value
 */
@RestController
@RequestMapping("/api/public/pages")
public class PublicPageContentController {

    private final PageContentRepository pageContentRepository;

    public PublicPageContentController(PageContentRepository pageContentRepository) {
        this.pageContentRepository = pageContentRepository;
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, String>> getAllPageContent() {
        Map<String, String> map = new HashMap<>();
        pageContentRepository.findAll().forEach(p -> map.put(p.getContentKey(), p.getContentValue()));
        return ResponseEntity.ok(map);
    }

    @GetMapping("/{key}")
    public ResponseEntity<String> getPageContent(@PathVariable String key) {
        return pageContentRepository.findByContentKey(key)
                .map(p -> ResponseEntity.ok(p.getContentValue()))
                .orElse(ResponseEntity.ok(""));
    }
}
