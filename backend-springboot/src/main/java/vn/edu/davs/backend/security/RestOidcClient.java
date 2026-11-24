package vn.edu.davs.backend.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import vn.edu.davs.backend.config.SecurityProperties.OidcProperties;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.common.exception.DavsException;

/**
 * Spring {@link RestClient}-based OIDC Authorization Code + PKCE client.
 */
@Component
@RequiredArgsConstructor
public class RestOidcClient implements OidcClient {

    private static final TypeReference<Map<String, Object>> JACKSON_MAP_TYPE = new TypeReference<>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> SPRING_MAP_TYPE =
            new ParameterizedTypeReference<>() { };

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Override
    public OidcUserIdentity exchangeCode(
            OidcProperties oidc,
            String code,
            String codeVerifier,
            String redirectUri
    ) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", redirectUri);
            form.add("client_id", oidc.getClientId());
            if (oidc.getClientSecret() != null && !oidc.getClientSecret().isBlank()) {
                form.add("client_secret", oidc.getClientSecret());
            }
            form.add("code_verifier", codeVerifier);

            Map<String, Object> tokenResponse = restClient.post()
                    .uri(oidc.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SPRING_MAP_TYPE);

            if (tokenResponse == null) {
                throw new DavsException(ErrorCode.ERR_AUTH_01);
            }

            if (oidc.getUserinfoUri() != null && !oidc.getUserinfoUri().isBlank()) {
                Object accessTokenObj = tokenResponse.get("access_token");
                if (accessTokenObj != null) {
                    Map<String, Object> userInfo = restClient.get()
                            .uri(oidc.getUserinfoUri())
                            .header("Authorization", "Bearer " + accessTokenObj)
                            .retrieve()
                            .body(SPRING_MAP_TYPE);
                    if (userInfo != null && userInfo.get("email") != null) {
                        return extractIdentity(userInfo);
                    }
                }
            }

            Object idTokenObj = tokenResponse.get("id_token");
            if (idTokenObj instanceof String idToken) {
                Map<String, Object> claims = decodeJwtPayload(idToken);
                return extractIdentity(claims);
            }
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        } catch (DavsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DavsException(ErrorCode.ERR_AUTH_01, ex);
        }
    }

    private OidcUserIdentity extractIdentity(Map<String, Object> claims) {
        String subject = claims.get("sub") != null ? String.valueOf(claims.get("sub")) : "";
        String email = claims.get("email") != null ? String.valueOf(claims.get("email")).trim() : "";
        Object verifiedObj = claims.get("email_verified");
        boolean emailVerified = verifiedObj == null || Boolean.parseBoolean(String.valueOf(verifiedObj));
        if (email.isBlank()) {
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }
        return new OidcUserIdentity(subject, email, emailVerified);
    }

    private Map<String, Object> decodeJwtPayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new DavsException(ErrorCode.ERR_AUTH_01);
        }
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), JACKSON_MAP_TYPE);
    }
}
