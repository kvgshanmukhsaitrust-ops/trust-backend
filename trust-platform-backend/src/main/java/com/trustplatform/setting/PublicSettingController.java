package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/settings")
public class PublicSettingController {

    private final SystemSettingService settingService;

    public PublicSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/hero-image")
    public ResponseEntity<String> getHeroImage() {
        String url = settingService.getSettingValue("HOME_HERO_IMAGE");
        return ResponseEntity.ok(url);
    }
}
