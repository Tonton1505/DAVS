package com.davs.datasource.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataType {
    NUMBER("Number"),
    STRING("String"),
    DATE("Date"),
    DATETIME("Date & Time"),
    BOOLEAN("Boolean"),
    GEOGRAPHIC("Geographic");

    private final String displayName;

    DataType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static DataType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return STRING;
        }
        String normalized = value.trim();
        for (DataType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        if (normalized.equalsIgnoreCase("Date and Time") || normalized.equalsIgnoreCase("DateTime")) {
            return DATETIME;
        }
        return STRING;
    }

    public boolean isCompatibleWith(DataType other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        return (this == DATE && other == DATETIME) || (this == DATETIME && other == DATE);
    }
}
