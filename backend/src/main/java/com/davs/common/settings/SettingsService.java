package com.davs.common.settings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Provides typed, cached access to configurable limits stored in {@code system_settings}.
 */
@Service
public class SettingsService {

    public static final String KEY_UPLOAD_MAX_BYTES = "upload.maxBytes";
    public static final String KEY_REFRESH_MAX_CONCURRENT = "refresh.maxConcurrent";

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("session.idleMinutes", "30"),
            Map.entry("login.maxFailures", "5"),
            Map.entry("login.lockMinutes", "15"),
            Map.entry(KEY_UPLOAD_MAX_BYTES, "1073741824"),
            Map.entry("workbook.maxVersions", "25"),
            Map.entry("export.maxRows", "500000"),
            Map.entry("export.maxBytes", "104857600"),
            Map.entry("chart.maxMarks", "100000"),
            Map.entry("trash.retentionDays", "30"),
            Map.entry("audit.retentionMonths", "12"),
            Map.entry("import.maxUsersPerFile", "1000"),
            Map.entry(KEY_REFRESH_MAX_CONCURRENT, "4"),
            Map.entry("refresh.pauseAfterFailures", "5"),
            Map.entry("comment.editWindowHours", "24"),
            Map.entry("share.maxRecipients", "100"),
            Map.entry("project.maxDepth", "5"),
            Map.entry("publish.publicEnabled", "false")
    );

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public SettingsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getString(String key, String fallback) {
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        String dbValue = queryFromDatabase(key);
        if (dbValue != null) {
            cache.put(key, dbValue);
            return dbValue;
        }
        return DEFAULTS.getOrDefault(key, fallback);
    }

    public int getInt(String key, int fallback) {
        String raw = getString(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public long getLong(String key, long fallback) {
        String raw = getString(key, String.valueOf(fallback));
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String raw = getString(key, String.valueOf(fallback));
        return Boolean.parseBoolean(raw.trim());
    }

    public void set(String key, String value) {
        cache.put(key, value);
        if (jdbcTemplate != null) {
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE system_settings SET setting_value = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?",
                        value,
                        key
                );
                if (updated == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO system_settings (setting_key, setting_value, value_type) VALUES (?, ?, 'STRING')",
                            key,
                            value
                    );
                }
            } catch (DataAccessException ignored) {
                // Keep in-memory cache updated when running without database table
            }
        }
    }

    public void evictCache() {
        cache.clear();
    }

    private String queryFromDatabase(String key) {
        if (jdbcTemplate == null) {
            return null;
        }
        try {
            List<String> rows = jdbcTemplate.queryForList(
                    "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                    String.class,
                    key
            );
            return rows.isEmpty() ? null : rows.get(0);
        } catch (DataAccessException ex) {
            return null;
        }
    }
}
