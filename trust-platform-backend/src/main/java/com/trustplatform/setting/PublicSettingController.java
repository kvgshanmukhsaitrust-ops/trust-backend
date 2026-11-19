package com.trustplatform.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/settings")
public class PublicSettingController {

    private final SystemSettingService settingService;
    private final SystemSettingRepository settingRepository;

    public PublicSettingController(SystemSettingService settingService,
                                   SystemSettingRepository settingRepository) {
        this.settingService = settingService;
        this.settingRepository = settingRepository;
    }

    @GetMapping("/hero-image")
    public ResponseEntity<String> getHeroImage() {
        String url = settingService.getSettingValue("HOME_HERO_IMAGE");
        return ResponseEntity.ok(url != null ? url : "");
    }

    /** Only CMS keys with this prefix are ever exposed publicly (SECURITY: allow-list, not "everything"). */
    private static final String PUBLIC_KEY_PREFIX = "HOME_";
    private static final java.util.regex.Pattern SENSITIVE =
            java.util.regex.Pattern.compile("SECRET|PASSWORD|TOKEN|PRIVATE|CREDENTIAL|API_?KEY", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Returns the PUBLIC (homepage CMS) settings as a key-value map.
     * Anything outside the allow-list, or that looks like a credential, is never returned.
     * Never throws — returns an empty map if no settings exist.
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        List<SystemSetting> all = settingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        all.stream()
                .filter(s -> s.getSettingKey() != null
                        && s.getSettingKey().startsWith(PUBLIC_KEY_PREFIX)
                        && !SENSITIVE.matcher(s.getSettingKey()).find())
                .forEach(s -> map.put(s.getSettingKey(), s.getSettingValue()));
        return ResponseEntity.ok(map);
    }
}
