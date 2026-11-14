package com.davs.common.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Canonical catalog of configurable system limits in {@code system_settings} (T-008).
 */
public enum SettingKey {

    SESSION_IDLE_MINUTES("session.idleMinutes", SettingValueType.INT, "30",
            "Idle session timeout in minutes before re-authentication is required"),
    LOGIN_MAX_FAILURES("login.maxFailures", SettingValueType.INT, "5",
            "Maximum consecutive failed login attempts before temporary lockout"),
    LOGIN_LOCK_MINUTES("login.lockMinutes", SettingValueType.INT, "15",
            "Account lockout duration in minutes after exceeding login.maxFailures"),
    UPLOAD_MAX_BYTES("upload.maxBytes", SettingValueType.LONG, "1073741824",
            "Maximum upload file size in bytes (1 GB)"),
    WORKBOOK_MAX_VERSIONS("workbook.maxVersions", SettingValueType.INT, "25",
            "Maximum retained versions per workbook"),
    EXPORT_MAX_ROWS("export.maxRows", SettingValueType.INT, "500000",
            "Maximum rows allowed in a single export"),
    EXPORT_MAX_BYTES("export.maxBytes", SettingValueType.LONG, "104857600",
            "Maximum export file size in bytes (100 MB)"),
    CHART_MAX_MARKS("chart.maxMarks", SettingValueType.INT, "100000",
            "Maximum marks rendered in a single chart"),
    TRASH_RETENTION_DAYS("trash.retentionDays", SettingValueType.INT, "30",
            "Number of days soft-deleted items remain in trash before purge"),
    AUDIT_RETENTION_MONTHS("audit.retentionMonths", SettingValueType.INT, "12",
            "Minimum retention period in months for audit_logs"),
    IMPORT_MAX_USERS_PER_FILE("import.maxUsersPerFile", SettingValueType.INT, "1000",
            "Maximum user rows allowed per bulk CSV/Excel import file"),
    REFRESH_MAX_CONCURRENT("refresh.maxConcurrent", SettingValueType.INT, "4",
            "Maximum concurrent extract refresh jobs"),
    REFRESH_PAUSE_AFTER_FAILURES("refresh.pauseAfterFailures", SettingValueType.INT, "5",
            "Consecutive refresh failures before auto-pausing schedule"),
    COMMENT_EDIT_WINDOW_HOURS("comment.editWindowHours", SettingValueType.INT, "24",
            "Hours after posting during which a comment can be edited"),
    SHARE_MAX_RECIPIENTS("share.maxRecipients", SettingValueType.INT, "100",
            "Maximum recipients per share action"),
    PROJECT_MAX_DEPTH("project.maxDepth", SettingValueType.INT, "5",
            "Maximum nesting depth of project folders"),
    PUBLISH_PUBLIC_ENABLED("publish.publicEnabled", SettingValueType.BOOLEAN, "false",
            "Whether public/anonymous publishing is enabled");

    private static final Map<String, SettingKey> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SettingKey::getKey, Function.identity()));

    private final String key;
    private final SettingValueType valueType;
    private final String defaultValue;
    private final String description;

    SettingKey(String key, SettingValueType valueType, String defaultValue, String description) {
        this.key = key;
        this.valueType = valueType;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public SettingValueType getValueType() {
        return valueType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultInt() {
        return Integer.parseInt(defaultValue);
    }

    public long getDefaultLong() {
        return Long.parseLong(defaultValue);
    }

    public boolean getDefaultBoolean() {
        return Boolean.parseBoolean(defaultValue);
    }

    public static Optional<SettingKey> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_KEY.get(key));
    }
}
