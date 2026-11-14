package vn.edu.davs.backend.dto;

/**
 * Supported connector descriptor returned by {@code GET /api/v1/connectors} (UC11).
 */
public record ConnectorDto(
        String id,
        String name,
        String category,
        boolean available,
        Integer defaultPort,
        String driverDownloadUrl
) {
}
