package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SettingValueType;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a configurable entry in {@code system_settings}.
 */
public record SystemSettingDto(
        String key,
        String value,
        SettingValueType valueType,
        String description,
        Instant updatedAt,
        UUID updatedBy
) {
}
