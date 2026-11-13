package com.davs.datasource.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FieldRole {
    DIMENSION("Dimension"),
    MEASURE("Measure");

    private final String displayName;

    FieldRole(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static FieldRole fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DIMENSION;
        }
        String normalized = value.trim();
        for (FieldRole role : values()) {
            if (role.name().equalsIgnoreCase(normalized) || role.displayName.equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return DIMENSION;
    }
}
