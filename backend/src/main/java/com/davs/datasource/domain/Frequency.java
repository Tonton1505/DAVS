package com.davs.datasource.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Frequency {
    HOURLY("Hằng giờ"),
    DAILY("Hằng ngày"),
    WEEKLY("Hằng tuần"),
    MONTHLY("Hằng tháng");

    private final String displayName;

    Frequency(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Frequency fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DAILY;
        }
        String normalized = value.trim();
        for (Frequency f : values()) {
            if (f.name().equalsIgnoreCase(normalized) || f.displayName.equalsIgnoreCase(normalized)) {
                return f;
            }
        }
        return DAILY;
    }
}
