package vn.edu.davs.backend.dto;

import java.util.UUID;

/**
 * Accepted response for asynchronous CSV user import ({@code POST /api/v1/users/import}).
 */
public record ImportJobResponse(UUID jobId) {
}
