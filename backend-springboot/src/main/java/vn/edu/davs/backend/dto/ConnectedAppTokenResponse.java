package vn.edu.davs.backend.dto;

/**
 * Response payload carrying an embed access token (UC26).
 */
public record ConnectedAppTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String trustType
) {
}
