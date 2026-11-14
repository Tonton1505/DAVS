package vn.edu.davs.backend.dto;

/**
 * Optional JSON body for {@code POST /api/v1/auth/refresh} when not using the HttpOnly cookie.
 */
public record RefreshRequest(String refreshToken) {
}
