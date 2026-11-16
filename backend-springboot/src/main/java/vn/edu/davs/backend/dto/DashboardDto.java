package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representation of a dashboard in a workbook (UC17 & UC18).
 */
public record DashboardDto(
        UUID id,
        UUID workbookId,
        String name,
        String description,
        String sizeType,
        Integer width,
        Integer height,
        List<DashboardTileDto> tiles,
        List<DashboardActionDto> actions,
        Instant createdAt,
        Instant updatedAt
) {
    public DashboardDto {
        tiles = tiles == null ? List.of() : List.copyOf(tiles);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
