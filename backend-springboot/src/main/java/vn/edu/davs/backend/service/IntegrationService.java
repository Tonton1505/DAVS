package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.IntegrationConnectResponse;
import vn.edu.davs.backend.dto.IntegrationDto;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC25 business logic for external platform OAuth 2.0 integrations (Slack, Salesforce):
 * enforces BUS-INT-01 (Site Admin only), BUS-INT-02 (OAuth 2.0 CSRF state &amp; AES-256-GCM encrypted tokens),
 * BUS-INT-03 (non-destructive disconnect), and ERR-INT-01 on authorization failure.
 */
public interface IntegrationService {


    @Transactional
    List<IntegrationDto> listIntegrations();


    @Transactional
    IntegrationConnectResponse initiateConnect(String rawPlatform, DavsUser actor);


    @Transactional
    IntegrationDto handleCallback(
            String rawPlatform,
            String code,
            String state,
            String error,
            String accountName,
            Long expiresInSeconds,
            DavsUser actor
    );


    @Transactional
    IntegrationDto disconnect(String rawPlatform, DavsUser actor);
}
