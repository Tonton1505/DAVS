package vn.edu.davs.backend.dto;

/**
 * Request payload for {@code POST /api/v1/auth/accept-terms} (VAL-AUTH-04).
 */
public record AcceptTermsRequest(boolean accepted) {
}
