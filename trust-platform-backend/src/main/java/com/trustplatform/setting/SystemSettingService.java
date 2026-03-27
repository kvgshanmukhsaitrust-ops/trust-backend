package com.trustplatform.setting;

import com.trustplatform.exception.ResourceNotFoundException;
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
     * Throws ResourceNotFoundException if the key does not exist.
     */
    public String getSettingValue(String key) {
        SystemSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + key));
        return setting.getSettingValue();
    }

    /**
     * Updates the value for the given setting key.
     * Returns the updated SystemSetting entity.
     */
    @Transactional
    public SystemSetting updateSetting(String key, String newValue) {
        SystemSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + key));
        setting.setSettingValue(newValue);
        return repository.save(setting);
    }
}
