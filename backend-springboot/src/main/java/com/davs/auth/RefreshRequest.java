package com.davs.auth;

/**
 * Optional JSON body for {@code POST /api/v1/auth/refresh} when not using the HttpOnly cookie.
 */
public record RefreshRequest(String refreshToken) {
}
