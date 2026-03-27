package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

    private final SystemSettingService settingService;

    public AdminSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @PutMapping("/hero-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateHeroImage(@RequestBody String newUrl) {
        SystemSetting updated = settingService.updateSetting("HOME_HERO_IMAGE", newUrl.trim());
        return ResponseEntity.ok(updated.getSettingValue());
    }
}
