package com.davs.common.service;

import com.davs.common.security.SecurityProperties;
import jakarta.validation.ConstraintViolationException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides cached, strongly-typed access to configurable runtime settings stored in
 * {@code system_settings} with canonical defaults from {@link SettingKey} (T-008).
 */
@Slf4j
@Service
public class SettingsService {

    public static final String SESSION_TIMEOUT_MINUTES_KEY = SettingKey.SESSION_IDLE_MINUTES.getKey();

    private static final String MISSING_SENTINEL = "__DAVS_MISSING_SETTING__";

    private final JdbcTemplate jdbcTemplate;
    private final SecurityProperties securityProperties;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Autowired
    public SettingsService(JdbcTemplate jdbcTemplate, SecurityProperties securityProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityProperties = securityProperties;
    }

    /**
     * Reads a raw string setting from cache or {@code system_settings}.
     */
    public Optional<String> getSetting(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String cached = cache.get(key);
        if (cached != null) {
            return MISSING_SENTINEL.equals(cached) ? Optional.empty() : Optional.of(cached);
        }
        Optional<String> fromDb = fetchFromDatabase(key);
        cache.put(key, fromDb.orElse(MISSING_SENTINEL));
        return fromDb;
    }

    public int getInt(SettingKey key) {
        return getIntSetting(key.getKey(), key.getDefaultInt());
    }

    public long getLong(SettingKey key) {
        return getLongSetting(key.getKey(), key.getDefaultLong());
    }

    public boolean getBoolean(SettingKey key) {
        return getBooleanSetting(key.getKey(), key.getDefaultBoolean());
    }

    public String getString(SettingKey key) {
        return getSetting(key.getKey()).orElse(key.getDefaultValue());
    }

    /**
     * Reads an integer setting with a fallback default if missing or malformed.
     */
    public int getIntSetting(String key, int defaultValue) {
        return getSetting(key)
                .map(String::trim)
                .flatMap(val -> {
                    try {
                        int parsed = Integer.parseInt(val);
                        return parsed > 0 ? Optional.of(parsed) : Optional.empty();
                    } catch (NumberFormatException ex) {
                        log.warn("Invalid integer value '{}' for setting '{}'; using default {}", val, key, defaultValue);
                        return Optional.empty();
                    }
                })
                .orElse(defaultValue);
    }

    public long getLongSetting(String key, long defaultValue) {
        return getSetting(key)
                .map(String::trim)
                .flatMap(val -> {
                    try {
                        long parsed = Long.parseLong(val);
                        return parsed > 0L ? Optional.of(parsed) : Optional.empty();
                    } catch (NumberFormatException ex) {
                        log.warn("Invalid long value '{}' for setting '{}'; using default {}", val, key, defaultValue);
                        return Optional.empty();
                    }
                })
                .orElse(defaultValue);
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return getSetting(key)
                .map(String::trim)
                .flatMap(val -> {
                    if ("true".equalsIgnoreCase(val)) {
                        return Optional.of(Boolean.TRUE);
                    }
                    if ("false".equalsIgnoreCase(val)) {
                        return Optional.of(Boolean.FALSE);
                    }
                    log.warn("Invalid boolean value '{}' for setting '{}'; using default {}", val, key, defaultValue);
                    return Optional.empty();
                })
                .orElse(defaultValue);
    }

    /**
     * Returns the idle session timeout in minutes (default 30 minutes per SRS).
     */
    public int getSessionTimeoutMinutes() {
        int fallback = securityProperties != null
                ? securityProperties.getSessionTimeoutMinutes()
                : SettingKey.SESSION_IDLE_MINUTES.getDefaultInt();
        return getIntSetting(SESSION_TIMEOUT_MINUTES_KEY, fallback);
    }

    /**
     * Returns the idle session timeout as a {@link Duration}.
     */
    public Duration getSessionTimeout() {
        return Duration.ofMinutes(getSessionTimeoutMinutes());
    }

    /**
     * Clears the in-memory settings cache so subsequent reads reload from PostgreSQL.
     */
    public void evictCache() {
        cache.clear();
    }

    /**
     * Lists all system settings ordered by key, merging DB rows with canonical defaults.
     */
    @Transactional(readOnly = true)
    public List<SystemSettingDto> listAllSettings() {
        Map<String, SystemSettingDto> merged = new LinkedHashMap<>();
        for (SettingKey key : SettingKey.values()) {
            merged.put(
                    key.getKey(),
                    new SystemSettingDto(
                            key.getKey(),
                            key.getDefaultValue(),
                            key.getValueType(),
                            key.getDescription(),
                            null,
                            null
                    )
            );
        }

        List<SystemSettingDto> dbRows = jdbcTemplate.query(
                "SELECT setting_key, setting_value, value_type, description, updated_at, updated_by "
                        + "FROM system_settings ORDER BY setting_key ASC",
                (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("updated_at");
                    return new SystemSettingDto(
                            rs.getString("setting_key"),
                            rs.getString("setting_value"),
                            parseValueType(rs.getString("value_type")),
                            rs.getString("description"),
                            ts != null ? ts.toInstant() : null,
                            rs.getObject("updated_by", UUID.class)
                    );
                }
        );

        for (SystemSettingDto row : dbRows) {
            merged.put(row.key(), row);
            cache.put(row.key(), row.value());
        }

        List<SystemSettingDto> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(SystemSettingDto::key));
        return result;
    }

    /**
     * Validates and persists setting updates, refreshing the in-memory cache immediately.
     */
    @Transactional
    public List<SystemSettingDto> updateSettings(Map<String, String> updates, UUID updatedBy) {
        if (updates == null || updates.isEmpty()) {
            throw new ConstraintViolationException("Settings map must not be empty", Set.of());
        }

        Instant now = Instant.now();
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String rawKey = entry.getKey();
            String rawValue = entry.getValue() != null ? entry.getValue().trim() : "";
            SettingKey settingKey = SettingKey.fromKey(rawKey)
                    .orElseThrow(() -> new ConstraintViolationException(
                            "Unknown setting key: " + rawKey,
                            Set.of()
                    ));
            String normalizedValue = validateAndNormalize(settingKey, rawValue);

            jdbcTemplate.update(
                    "INSERT INTO system_settings (setting_key, setting_value, value_type, description, updated_at, updated_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT (setting_key) DO UPDATE SET "
                            + "setting_value = EXCLUDED.setting_value, "
                            + "updated_at = EXCLUDED.updated_at, "
                            + "updated_by = EXCLUDED.updated_by",
                    settingKey.getKey(),
                    normalizedValue,
                    settingKey.getValueType().name(),
                    settingKey.getDescription(),
                    Timestamp.from(now),
                    updatedBy
            );
            cache.put(settingKey.getKey(), normalizedValue);
        }

        return listAllSettings();
    }

    private Optional<String> fetchFromDatabase(String key) {
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                    (rs, rowNum) -> rs.getString("setting_value"),
                    key
            );
            return rows.stream().findFirst();
        } catch (DataAccessException ex) {
            log.warn("Unable to read setting '{}' from database; falling back to default: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private static SettingValueType parseValueType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return SettingValueType.STRING;
        }
        try {
            return SettingValueType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SettingValueType.STRING;
        }
    }

    private static String validateAndNormalize(SettingKey key, String rawValue) {
        return switch (key.getValueType()) {
            case INT -> {
                try {
                    int val = Integer.parseInt(rawValue);
                    if (val <= 0) {
                        throw new ConstraintViolationException(
                                key.getKey() + " must be a positive integer",
                                Set.of()
                        );
                    }
                    yield String.valueOf(val);
                } catch (NumberFormatException ex) {
                    throw new ConstraintViolationException(
                            key.getKey() + " must be a valid integer",
                            Set.of()
                    );
                }
            }
            case LONG -> {
                try {
                    long val = Long.parseLong(rawValue);
                    if (val <= 0L) {
                        throw new ConstraintViolationException(
                                key.getKey() + " must be a positive long",
                                Set.of()
                        );
                    }
                    yield String.valueOf(val);
                } catch (NumberFormatException ex) {
                    throw new ConstraintViolationException(
                            key.getKey() + " must be a valid long",
                            Set.of()
                    );
                }
            }
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                    throw new ConstraintViolationException(
                            key.getKey() + " must be true or false",
                            Set.of()
                    );
                }
                yield String.valueOf(Boolean.parseBoolean(rawValue));
            }
            case STRING -> rawValue;
        };
    }
}
