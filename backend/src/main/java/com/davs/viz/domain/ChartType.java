package com.davs.viz.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChartType {
    BAR("Cột"),
    LINE("Đường"),
    PIE("Tròn"),
    SCATTER("Phân tán"),
    AREA("Vùng"),
    CROSS_TAB("Bảng chéo");

    private final String displayName;

    ChartType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ChartType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BAR;
        }
        String normalized = value.trim();
        for (ChartType t : values()) {
            if (t.name().equalsIgnoreCase(normalized) || t.displayName.equalsIgnoreCase(normalized)) {
                return t;
            }
        }
        return BAR;
    }
}
