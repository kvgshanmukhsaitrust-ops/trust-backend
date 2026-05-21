package com.trustplatform.setting;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the current value for the given setting key.
     * Returns null (not exception) if key not found — callers handle missing gracefully.
     */
    @Cacheable(value = "cms", key = "#key")
    public String getSettingValue(String key) {
        return repository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    /**
     * Upserts (creates or updates) a setting key with the given value.
     */
    @Transactional
    @CacheEvict(value = "cms", key = "#key")
    public SystemSetting upsertSetting(String key, String newValue) {
        SystemSetting setting = repository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setSettingValue(newValue);
        return repository.save(setting);
    }
}
