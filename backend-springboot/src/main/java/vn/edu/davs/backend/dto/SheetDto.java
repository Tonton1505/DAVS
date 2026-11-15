package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.ChartType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Representation of a visualization sheet in a workbook (UC15).
 */
public record SheetDto(
        UUID id,
        UUID workbookId,
        String name,
        ChartType chartType,
        Map<String, Object> definition,
        TooltipConfigDto tooltipConfig,
        Instant createdAt,
        Instant updatedAt
) {
}
