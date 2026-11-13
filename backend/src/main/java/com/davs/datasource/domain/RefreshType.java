package com.davs.datasource.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RefreshType {
    FULL("Đầy đủ"),
    INCREMENTAL("Tăng dần");

    private final String displayName;

    RefreshType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static RefreshType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FULL;
        }
        String normalized = value.trim();
        for (RefreshType t : values()) {
            if (t.name().equalsIgnoreCase(normalized) || t.displayName.equalsIgnoreCase(normalized)) {
                return t;
            }
        }
        return FULL;
    }
}
