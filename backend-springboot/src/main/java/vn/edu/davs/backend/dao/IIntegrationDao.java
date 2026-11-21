package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.IntegrationDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for UC25 external platform integrations ({@code integrations}).
 */
public interface IIntegrationDao {


    void markExpiredTokensReconnectRequired();


    List<IntegrationDto> listAll();


    Optional<IntegrationDto> findByPlatform(String platform);


    Optional<String> findOauthState(String platform);


    Optional<String> findEncryptedAccessToken(String platform);


    void saveOauthState(String platform, String state);


    void markConnected(
            String platform,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String accountName,
            Instant tokenExpiresAt,
            UUID connectedBy
    );


    void markDisconnected(String platform);
}
