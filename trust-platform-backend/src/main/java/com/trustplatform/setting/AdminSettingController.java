package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingController {

    private final SystemSettingService settingService;
    private final SystemSettingRepository settingRepository;

    public AdminSettingController(SystemSettingService settingService,
                                   SystemSettingRepository settingRepository) {
        this.settingService = settingService;
        this.settingRepository = settingRepository;
    }

    // Legacy endpoint: update hero image
    @PutMapping("/hero-image")
    public ResponseEntity<String> updateHeroImage(@RequestBody(required = false) String newUrl) {
        String cleaned = newUrl == null ? "" : newUrl.trim();
        SystemSetting updated = settingService.upsertSetting("HOME_HERO_IMAGE", cleaned);
        return ResponseEntity.ok(updated.getSettingValue());
    }

    // Get all settings for admin
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSettings() {
        List<SystemSetting> all = settingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        all.forEach(s -> map.put(s.getSettingKey(), s.getSettingValue()));
        return ResponseEntity.ok(map);
    }

    // Upsert any setting key — value comes as plain JSON string
    @PutMapping("/{key}")
    public ResponseEntity<String> upsertSetting(
            @PathVariable String key,
            @RequestBody(required = false) String value) {
        // Strip surrounding quotes if the client sent a JSON-encoded string
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        settingService.upsertSetting(key, cleaned);
        return ResponseEntity.ok(cleaned);
    }

    // Delete a setting key
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteSetting(@PathVariable String key) {
        settingRepository.findBySettingKey(key)
                .ifPresent(settingRepository::delete);
        return ResponseEntity.noContent().build();
    }
}
