package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.SystemSettingDto;
import vn.edu.davs.backend.enu.SettingKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides cached, strongly-typed access to configurable runtime settings stored in
 * {@code system_settings} with canonical defaults from {@link SettingKey} (T-008).
 */
public interface SettingsService {


    public static final String SESSION_TIMEOUT_MINUTES_KEY = SettingKey.SESSION_IDLE_MINUTES.getKey();


    /**
     * Reads a raw string setting from cache or {@code system_settings}.
     */
    Optional<String> getSetting(String key);


    int getInt(SettingKey key);


    long getLong(SettingKey key);


    boolean getBoolean(SettingKey key);


    String getString(SettingKey key);


    /**
     * Reads an integer setting with a fallback default if missing or malformed.
     */
    int getIntSetting(String key, int defaultValue);


    long getLongSetting(String key, long defaultValue);


    boolean getBooleanSetting(String key, boolean defaultValue);


    /**
     * Returns the idle session timeout in minutes (default 30 minutes per SRS).
     */
    int getSessionTimeoutMinutes();


    /**
     * Returns the idle session timeout as a {@link Duration}.
     */
    Duration getSessionTimeout();


    /**
     * Clears the in-memory settings cache so subsequent reads reload from PostgreSQL.
     */
    void evictCache();


    /**
     * Lists all system settings ordered by key, merging DB rows with canonical defaults.
     */
    @Transactional(readOnly = true)
    List<SystemSettingDto> listAllSettings();


    /**
     * Validates and persists setting updates, refreshing the in-memory cache immediately.
     */
    @Transactional
    List<SystemSettingDto> updateSettings(Map<String, String> updates, UUID updatedBy);
}
