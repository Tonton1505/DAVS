package vn.edu.davs.backend.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration properties bound to {@code davs.security.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "davs.security")
public class SecurityProperties {

    private String jwtSecret;
    private String encryptionKey;
    private long accessTokenTtlMinutes = 15L;
    private int sessionTimeoutMinutes = 30;
    private CorsProperties cors = new CorsProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();
    private OidcProperties oidc = new OidcProperties();
    private MfaProperties mfa = new MfaProperties();

    @Getter
    @Setter
    public static class CorsProperties {
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Request-Id");
        private boolean allowCredentials = true;
        private long maxAgeSeconds = 3600L;
    }

    @Getter
    @Setter
    public static class RateLimitProperties {
        private int authMaxRequests = 20;
        private int authWindowSeconds = 60;
    }

    @Getter
    @Setter
    public static class OidcProperties {
        private boolean enabled = false;
        private String providerName = "SSO";
        private String clientId = "";
        private String clientSecret = "";
        private String authorizationUri = "";
        private String tokenUri = "";
        private String userinfoUri = "";
        private String redirectUri = "http://localhost:3000/login/oidc/callback";
        private String scopes = "openid email profile";
    }

    @Getter
    @Setter
    public static class MfaProperties {
        private boolean enabled = true;
        private boolean required = false;
        private String issuer = "DAVS";
        private int allowedTimeStepDrift = 1;
    }
}
