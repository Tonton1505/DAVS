package com.davs.datasource.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JoinType {
    RELATIONSHIP("Relationship"),
    INNER("Inner"),
    LEFT("Left"),
    RIGHT("Right"),
    FULL("Full"),
    BLEND("Blend");

    private final String displayName;

    JoinType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static JoinType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return RELATIONSHIP;
        }
        String normalized = value.trim();
        for (JoinType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return RELATIONSHIP;
    }
}
