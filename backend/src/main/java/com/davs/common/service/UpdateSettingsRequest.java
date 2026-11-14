package com.davs.common.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request body for {@code PUT /api/v1/admin/settings}.
 */
public record UpdateSettingsRequest(
        @NotEmpty Map<@NotBlank String, @NotNull String> settings
) {
}
