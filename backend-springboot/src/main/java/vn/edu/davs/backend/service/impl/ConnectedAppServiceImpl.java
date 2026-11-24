package vn.edu.davs.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.ConnectedAppDto;
import vn.edu.davs.backend.dto.ConnectedAppTokenRequest;
import vn.edu.davs.backend.dto.ConnectedAppTokenResponse;
import vn.edu.davs.backend.dto.CreateConnectedAppRequest;
import vn.edu.davs.backend.dto.UpdateConnectedAppRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.common.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.service.ConnectedAppService;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.PermissionService;

@RequiredArgsConstructor
@Slf4j
@Service
public class ConnectedAppServiceImpl implements ConnectedAppService {


    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}|localhost$"
    );

    private static final SecureRandom RANDOM = new SecureRandom();


    private final JdbcTemplate jdbcTemplate;

    private final PermissionService permissionService;

    private final AuditService auditService;

    private final ObjectMapper objectMapper;


    /**
     * Creates a new Connected App (Direct Trust or OAuth 2.0 Trust).
     */
    @Transactional
    @Override
    public ConnectedAppDto createConnectedApp(CreateConnectedAppRequest request, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        Objects.requireNonNull(request, "request must not be null");

        String name = request.name() != null ? request.name().trim() : "";
        if (name.isBlank()) {
            throw new DavsException(ErrorCode.ERR_INT_02);
        }

        // 1. ERR-INT-03: Check duplicate app name
        if (isAppNameDuplicate(name, null)) {
            throw new DavsException(ErrorCode.ERR_INT_03);
        }

        String trustType = request.trustType() != null && !request.trustType().isBlank()
                ? request.trustType().trim().toUpperCase()
                : "DIRECT_TRUST";

        List<String> allowedDomains = request.allowedDomains() != null ? request.allowedDomains() : List.of();
        String appUrl = request.appUrl() != null ? request.appUrl().trim() : null;

        // 2. ERR-INT-02: Domain & URL validation
        validateDomainsAndUrl(trustType, allowedDomains, appUrl);

        // 3. BUS-INT-04: Generate Client ID and Secret (secret shown once, stored hashed)
        String clientId = "davs_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String rawSecret = "sec_" + generateRandomString(32);
        String secretHash = hashSecret(rawSecret);

        UUID appId = UUID.randomUUID();
        String domainsJson = writeJson(allowedDomains);
        String permissionScope = request.permissionScope() != null ? request.permissionScope().trim().toUpperCase() : "ALL";

        String insertSql = """
                INSERT INTO connected_apps (
                    id, name, trust_type, status, client_id, client_secret_hash,
                    allowed_domains, app_url, permission_scope, project_id, run_as_user_id,
                    created_by, created_at, updated_at
                ) VALUES (?, ?, ?, 'ENABLED', ?, ?, ?::jsonb, ?, ?, ?, ?, ?, now(), now())
                """;

        jdbcTemplate.update(
                insertSql,
                appId,
                name,
                trustType,
                clientId,
                secretHash,
                domainsJson,
                appUrl,
                permissionScope,
                request.projectId(),
                request.runAsUserId(),
                actor.getId()
        );

        auditService.log(AuditEvent.of(
                "CONNECTED_APP_CREATED",
                actor.getId(),
                actor.getEmail(),
                "CONNECTED_APP",
                appId.toString()
        ));

        return new ConnectedAppDto(
                appId,
                name,
                trustType,
                "ENABLED",
                clientId,
                rawSecret, // Plain secret returned ONLY once at creation
                allowedDomains,
                appUrl,
                request.projectId(),
                permissionScope,
                request.runAsUserId(),
                null,
                Instant.now(),
                Instant.now()
        );
    }


    /**
     * Lists all connected apps. Secrets are NOT included.
     */
    @Override
    public List<ConnectedAppDto> listConnectedApps(DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);

        return jdbcTemplate.query("""
                SELECT id, name, trust_type, status, client_id, allowed_domains,
                       app_url, permission_scope, project_id, run_as_user_id,
                       secret_rotated_at, created_at, updated_at
                FROM connected_apps
                ORDER BY created_at DESC
                """, this::mapRowToDto);
    }


    /**
     * Gets a single connected app by ID.
     */
    @Override
    public ConnectedAppDto getConnectedApp(UUID id, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);

        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, name, trust_type, status, client_id, allowed_domains,
                           app_url, permission_scope, project_id, run_as_user_id,
                           secret_rotated_at, created_at, updated_at
                    FROM connected_apps
                    WHERE id = ?
                    """, this::mapRowToDto, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
    }


    /**
     * Rotates client secret (BUS-INT-04: Old secret invalidated, new secret returned once).
     */
    @Transactional
    @Override
    public ConnectedAppDto rotateSecret(UUID id, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);

        ConnectedAppDto app = getConnectedApp(id, actor);

        String newSecret = "sec_" + generateRandomString(32);
        String secretHash = hashSecret(newSecret);

        jdbcTemplate.update("""
                UPDATE connected_apps
                SET client_secret_hash = ?, secret_rotated_at = now(), updated_at = now()
                WHERE id = ?
                """, secretHash, id);

        // Invalidate active tokens derived from old secret
        revokeAppTokens(id);

        auditService.log(AuditEvent.of(
                "CONNECTED_APP_SECRET_ROTATED",
                actor.getId(),
                actor.getEmail(),
                "CONNECTED_APP",
                id.toString()
        ));

        return new ConnectedAppDto(
                app.id(),
                app.name(),
                app.trustType(),
                app.status(),
                app.clientId(),
                newSecret, // Returned once
                app.allowedDomains(),
                app.appUrl(),
                app.projectId(),
                app.permissionScope(),
                app.runAsUserId(),
                Instant.now(),
                app.createdAt(),
                Instant.now()
        );
    }


    /**
     * Updates connected app status or config (BUS-INT-06: Disabling revokes active tokens).
     */
    @Transactional
    @Override
    public ConnectedAppDto updateConnectedApp(UUID id, UpdateConnectedAppRequest request, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        Objects.requireNonNull(request, "request must not be null");

        ConnectedAppDto app = getConnectedApp(id, actor);

        String status = request.status() != null ? request.status().trim().toUpperCase() : app.status();
        List<String> domains = request.allowedDomains() != null ? request.allowedDomains() : app.allowedDomains();
        String appUrl = request.appUrl() != null ? request.appUrl() : app.appUrl();

        validateDomainsAndUrl(app.trustType(), domains, appUrl);

        jdbcTemplate.update("""
                UPDATE connected_apps
                SET status = ?, allowed_domains = ?::jsonb, app_url = ?, updated_at = now()
                WHERE id = ?
                """, status, writeJson(domains), appUrl, id);

        // BUS-INT-06: When disabled, immediately revoke all active tokens
        if ("DISABLED".equalsIgnoreCase(status)) {
            revokeAppTokens(id);
        }

        auditService.log(AuditEvent.of(
                "CONNECTED_APP_UPDATED",
                actor.getId(),
                actor.getEmail(),
                "CONNECTED_APP",
                id.toString()
        ));

        return getConnectedApp(id, actor);
    }


    /**
     * Deletes connected app (BUS-INT-06: Immediately revokes all active tokens).
     */
    @Transactional
    @Override
    public void deleteConnectedApp(UUID id, DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);

        revokeAppTokens(id);

        int rows = jdbcTemplate.update("DELETE FROM connected_apps WHERE id = ?", id);
        if (rows == 0) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        auditService.log(AuditEvent.of(
                "CONNECTED_APP_DELETED",
                actor.getId(),
                actor.getEmail(),
                "CONNECTED_APP",
                id.toString()
        ));
    }


    /**
     * Generates an embed access token for a connected app (BUS-INT-05).
     */
    @Transactional
    @Override
    public ConnectedAppTokenResponse generateToken(ConnectedAppTokenRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        if (request.clientId() == null || request.clientSecret() == null) {
            throw new DavsException(ErrorCode.ERR_INT_02);
        }

        // 1. Look up app by clientId
        Map<String, Object> app;
        try {
            app = jdbcTemplate.queryForMap("""
                    SELECT id, trust_type, status, client_secret_hash, allowed_domains, run_as_user_id
                    FROM connected_apps
                    WHERE client_id = ?
                    """, request.clientId());
        } catch (EmptyResultDataAccessException ex) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        // Check enabled
        if (!"ENABLED".equalsIgnoreCase((String) app.get("status"))) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        // Verify secret hash
        String expectedHash = (String) app.get("client_secret_hash");
        if (!expectedHash.equals(hashSecret(request.clientSecret()))) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        // 2. BUS-INT-05: Exact domain matching for Direct Trust
        String trustType = (String) app.get("trust_type");
        List<String> allowedDomains = readJsonList((String) app.get("allowed_domains"));
        if ("DIRECT_TRUST".equalsIgnoreCase(trustType)) {
            String domain = request.domain() != null ? request.domain().trim().toLowerCase() : "";
            if (domain.isBlank() || !allowedDomains.contains(domain)) {
                throw new DavsException(ErrorCode.ERR_INT_02);
            }
        }

        // 3. Issue token
        UUID appId = (UUID) app.get("id");
        UUID runAsUser = (UUID) app.get("run_as_user_id");
        String rawToken = "davs_jwt_" + generateRandomString(48);
        String tokenHash = hashSecret(rawToken);
        long expiresInSeconds = 3600; // 1 hour
        Instant expiresAt = Instant.now().plus(expiresInSeconds, ChronoUnit.SECONDS);

        jdbcTemplate.update("""
                INSERT INTO connected_app_tokens (
                    id, app_id, token_hash, user_id, domain, expires_at, revoked, created_at
                ) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, FALSE, now())
                """,
                appId, tokenHash, runAsUser, request.domain(), expiresAt
        );

        return new ConnectedAppTokenResponse(
                rawToken,
                "Bearer",
                expiresInSeconds,
                trustType
        );
    }


    private void validateDomainsAndUrl(String trustType, List<String> domains, String appUrl) {
        if ("DIRECT_TRUST".equalsIgnoreCase(trustType)) {
            if (domains == null || domains.isEmpty()) {
                throw new DavsException(ErrorCode.ERR_INT_02);
            }
            for (String d : domains) {
                if (d == null || d.isBlank() || !isValidDomain(d.trim())) {
                    throw new DavsException(ErrorCode.ERR_INT_02);
                }
            }
        } else if ("OAUTH2_TRUST".equalsIgnoreCase(trustType)) {
            if (appUrl == null || appUrl.isBlank()) {
                throw new DavsException(ErrorCode.ERR_INT_02);
            }
            try {
                URI uri = URI.create(appUrl);
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    throw new DavsException(ErrorCode.ERR_INT_02);
                }
                if (uri.getHost() == null || uri.getHost().isBlank()) {
                    throw new DavsException(ErrorCode.ERR_INT_02);
                }
            } catch (Exception ex) {
                throw new DavsException(ErrorCode.ERR_INT_02);
            }
        }
    }


    private boolean isValidDomain(String domain) {
        String clean = domain.toLowerCase().trim();
        if (clean.contains("://") || clean.contains("/") || clean.contains(" ")) {
            return false;
        }
        return DOMAIN_PATTERN.matcher(clean).matches();
    }


    private boolean isAppNameDuplicate(String name, UUID excludeId) {
        try {
            String sql = "SELECT count(*) FROM connected_apps WHERE lower(name) = lower(?)";
            if (excludeId != null) {
                sql += " AND id <> '" + excludeId + "'";
            }
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }


    private void revokeAppTokens(UUID appId) {
        jdbcTemplate.update("UPDATE connected_app_tokens SET revoked = TRUE WHERE app_id = ?", appId);
    }


    private String hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }


    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }


    private ConnectedAppDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        Instant rotated = rs.getTimestamp("secret_rotated_at") != null
                ? rs.getTimestamp("secret_rotated_at").toInstant() : null;
        return new ConnectedAppDto(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("trust_type"),
                rs.getString("status"),
                rs.getString("client_id"),
                null, // Secret is masked after creation
                readJsonList(rs.getString("allowed_domains")),
                rs.getString("app_url"),
                rs.getObject("project_id", UUID.class),
                rs.getString("permission_scope"),
                rs.getObject("run_as_user_id", UUID.class),
                rotated,
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }


    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }


    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
