package vn.edu.davs.backend.dto;

/**
 * Request payload for obtaining an embed token via Connected App (UC26).
 */
public record ConnectedAppTokenRequest(
        String clientId,
        String clientSecret,
        String domain
) {
}
