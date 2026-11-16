package vn.edu.davs.backend.dto;

import java.util.UUID;

/**
 * Tile layout representation on a 12-column grid dashboard (UC17).
 */
public record DashboardTileDto(
        String id,
        UUID sheetId,
        String title,
        String type,
        int x,
        int y,
        int w,
        int h,
        String content
) {
}
