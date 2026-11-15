package vn.edu.davs.backend.dto;

import java.util.List;

/**
 * Custom tooltip configuration for a visualization sheet (UC18).
 */
public record TooltipConfigDto(
        String template,
        List<String> fields
) {
    public TooltipConfigDto {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
