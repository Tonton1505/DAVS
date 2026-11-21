package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.AcceptTermsRequest;
import vn.edu.davs.backend.dto.AuthTokenResponse;
import vn.edu.davs.backend.dto.AuthUserDto;
import vn.edu.davs.backend.dto.LoginRequest;
import vn.edu.davs.backend.dto.RefreshRequest;
import vn.edu.davs.backend.service.AuthService;
import vn.edu.davs.backend.service.OidcMfaService;
import vn.edu.davs.backend.service.OidcMfaService.MfaSetupResponse;
import vn.edu.davs.backend.service.OidcMfaService.MfaStatusResponse;
import vn.edu.davs.backend.service.OidcMfaService.OidcAuthorizeResponse;
import vn.edu.davs.backend.service.OidcMfaService.SsoConfigResponse;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.AuthRest;

@RequiredArgsConstructor
@RestController
public class AuthRestImpl implements AuthRest {


    private final AuthService authService;

    private final OidcMfaService oidcMfaService;

    @Override
    public ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = resolveClientIp(httpRequest);
        AuthService.AuthSessionResult result = authService.login(request, clientIp);
        if (!result.response().mfaRequired() && result.rawRefreshToken() != null) {
            addRefreshCookie(httpResponse, result.rawRefreshToken(), result.refreshTtlSeconds(), httpRequest.isSecure());
        }
        return ResponseEntity.ok(result.response());
    }

    @Override
    public ResponseEntity<SsoConfigResponse> ssoConfig() {
        return ResponseEntity.ok(oidcMfaService.getSsoConfig());
    }

    @Override
    public ResponseEntity<OidcAuthorizeResponse> oidcAuthorize(
            @RequestParam(value = "redirectUri", required = false) String redirectUri
    ) {
        return ResponseEntity.ok(oidcMfaService.buildOidcAuthorizeUrl(redirectUri));
    }

    @Override
    public ResponseEntity<AuthTokenResponse> oidcCallback(
            @Valid @RequestBody OidcCallbackRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = resolveClientIp(httpRequest);
        AuthService.AuthSessionResult result = oidcMfaService.handleOidcCallback(
                request.code(),
                request.state(),
                request.redirectUri(),
                clientIp
        );
        if (!result.response().mfaRequired() && result.rawRefreshToken() != null) {
            addRefreshCookie(httpResponse, result.rawRefreshToken(), result.refreshTtlSeconds(), httpRequest.isSecure());
        }
        return ResponseEntity.ok(result.response());
    }

    @Override
    public ResponseEntity<AuthTokenResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = resolveClientIp(httpRequest);
        AuthService.AuthSessionResult result = oidcMfaService.verifyMfaLogin(
                request.mfaToken(),
                request.code(),
                clientIp
        );
        addRefreshCookie(httpResponse, result.rawRefreshToken(), result.refreshTtlSeconds(), httpRequest.isSecure());
        return ResponseEntity.ok(result.response());
    }

    @Override
    public ResponseEntity<MfaStatusResponse> mfaStatus(@AuthenticationPrincipal DavsUser principal) {
        if (principal == null) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        return ResponseEntity.ok(oidcMfaService.getMfaStatus(principal.getId()));
    }

    @Override
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal DavsUser principal) {
        if (principal == null) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        return ResponseEntity.ok(oidcMfaService.setupMfa(principal.getId()));
    }

    @Override
    public ResponseEntity<MfaStatusResponse> enableMfa(
            @AuthenticationPrincipal DavsUser principal,
            @Valid @RequestBody MfaCodeRequest request
    ) {
        if (principal == null) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        return ResponseEntity.ok(oidcMfaService.enableMfa(principal.getId(), request.code()));
    }

    @Override
    public ResponseEntity<MfaStatusResponse> disableMfa(
            @AuthenticationPrincipal DavsUser principal,
            @Valid @RequestBody MfaCodeRequest request
    ) {
        if (principal == null) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        return ResponseEntity.ok(oidcMfaService.disableMfa(principal.getId(), request.code()));
    }

    @Override
    public ResponseEntity<AuthUserDto> acceptTerms(
            @AuthenticationPrincipal DavsUser principal,
            @RequestBody AcceptTermsRequest request
    ) {
        if (principal == null) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        AuthUserDto updated = authService.acceptTerms(principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<AuthTokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawRefreshToken = resolveRefreshToken(request, httpRequest);
        AuthService.AuthSessionResult result = authService.refresh(rawRefreshToken);
        addRefreshCookie(httpResponse, result.rawRefreshToken(), result.refreshTtlSeconds(), httpRequest.isSecure());
        return ResponseEntity.ok(result.response());
    }

    @Override
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal DavsUser principal,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawRefreshToken = resolveRefreshToken(request, httpRequest);
        authService.logout(rawRefreshToken, principal);
        clearRefreshCookie(httpResponse, httpRequest.isSecure());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AuthUserDto> me(@AuthenticationPrincipal DavsUser principal) {
        if (principal == null) {
            throw new DavsException(ErrorCode.ERR_AUTH_05);
        }
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }


    private static void addRefreshCookie(
            HttpServletResponse response,
            String rawRefreshToken,
            long maxAgeSeconds,
            boolean secure
    ) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }


    private static void clearRefreshCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }


    private static String resolveRefreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if (REFRESH_COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return request != null ? request.refreshToken() : null;
    }


    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
