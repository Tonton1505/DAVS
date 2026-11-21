package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.ConnectedAppDto;
import vn.edu.davs.backend.dto.ConnectedAppTokenRequest;
import vn.edu.davs.backend.dto.ConnectedAppTokenResponse;
import vn.edu.davs.backend.dto.CreateConnectedAppRequest;
import vn.edu.davs.backend.dto.UpdateConnectedAppRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing UC26: Connected Applications Management (FR-INT-01, BUS-INT-04..06).
 */
public interface ConnectedAppService {


    /**
     * Creates a new Connected App (Direct Trust or OAuth 2.0 Trust).
     */
    @Transactional
    ConnectedAppDto createConnectedApp(CreateConnectedAppRequest request, DavsUser actor);


    /**
     * Lists all connected apps. Secrets are NOT included.
     */
    List<ConnectedAppDto> listConnectedApps(DavsUser actor);


    /**
     * Gets a single connected app by ID.
     */
    ConnectedAppDto getConnectedApp(UUID id, DavsUser actor);


    /**
     * Rotates client secret (BUS-INT-04: Old secret invalidated, new secret returned once).
     */
    @Transactional
    ConnectedAppDto rotateSecret(UUID id, DavsUser actor);


    /**
     * Updates connected app status or config (BUS-INT-06: Disabling revokes active tokens).
     */
    @Transactional
    ConnectedAppDto updateConnectedApp(UUID id, UpdateConnectedAppRequest request, DavsUser actor);


    /**
     * Deletes connected app (BUS-INT-06: Immediately revokes all active tokens).
     */
    @Transactional
    void deleteConnectedApp(UUID id, DavsUser actor);


    /**
     * Generates an embed access token for a connected app (BUS-INT-05).
     */
    @Transactional
    ConnectedAppTokenResponse generateToken(ConnectedAppTokenRequest request);
}
