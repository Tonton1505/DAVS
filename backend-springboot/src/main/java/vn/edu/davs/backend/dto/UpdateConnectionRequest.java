package vn.edu.davs.backend.dto;

/**
 * Request body for {@code PUT /api/v1/datasources/{id}/connection} (UC14).
 */
public record UpdateConnectionRequest(
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String username,
        String password
) {
}
