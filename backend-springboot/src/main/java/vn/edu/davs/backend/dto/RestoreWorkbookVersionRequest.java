package vn.edu.davs.backend.dto;

/**
 * Request body for {@code POST /api/v1/workbooks/{id}/versions/{v}/restore} (UC10).
 */
public record RestoreWorkbookVersionRequest(
        String note
) {
}
