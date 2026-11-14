package vn.edu.davs.backend.security;

import vn.edu.davs.backend.config.SecurityProperties;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;

import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Issues and validates stateless HMAC-SHA256 JWT access tokens (default TTL: 15 minutes).
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Clock clock;

    @Autowired
    public JwtService(SecurityProperties securityProperties) {
        this(securityProperties, Clock.systemUTC());
    }

    public JwtService(SecurityProperties securityProperties, Clock clock) {
        this.signingKey = resolveSigningKey(securityProperties.getJwtSecret());
        this.accessTokenTtl = Duration.ofMinutes(securityProperties.getAccessTokenTtlMinutes());
        this.clock = clock;
    }

    /**
     * Generates a signed JWT access token for the given {@link DavsUser}.
     */
    public String generateAccessToken(DavsUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(accessTokenTtl);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_FULL_NAME, user.getFullName())
                .claim(CLAIM_ROLE, user.getSiteRole().name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and validates a JWT access token, returning the reconstructed {@link DavsUser}.
     *
     * @throws DavsException with {@link ErrorCode#ERR_AUTH_05} if missing, expired, or invalid
     */
    public DavsUser parseAndValidateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
                throw new DavsException(ErrorCode.ERR_AUTH_05);
            }

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            String fullName = claims.get(CLAIM_FULL_NAME, String.class);
            SiteRole role = SiteRole.valueOf(claims.get(CLAIM_ROLE, String.class));

            return DavsUser.of(userId, email, fullName, role);
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new DavsException(ErrorCode.ERR_AUTH_05, ex);
        }
    }

    private static SecretKey resolveSigningKey(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("davs.security.jwt-secret must be configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(configuredSecret);
        } catch (IllegalArgumentException ex) {
            keyBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("davs.security.jwt-secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
