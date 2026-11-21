package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.AcceptTermsRequest;
import vn.edu.davs.backend.dto.AuthTokenResponse;
import vn.edu.davs.backend.dto.AuthUserDto;
import vn.edu.davs.backend.dto.LoginRequest;
import vn.edu.davs.backend.dto.RefreshRequest;
import vn.edu.davs.backend.service.OidcMfaService.MfaSetupResponse;
import vn.edu.davs.backend.service.OidcMfaService.MfaStatusResponse;
import vn.edu.davs.backend.service.OidcMfaService.OidcAuthorizeResponse;
import vn.edu.davs.backend.service.OidcMfaService.SsoConfigResponse;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Authentication REST endpoints for UC01 ({@code /api/v1/auth/*}).
 */
@RequestMapping("/api/v1/auth")
public interface AuthRest {


    public static final String REFRESH_COOKIE_NAME = "davs_refresh_token";


    @PostMapping("/login")
    ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );


    @GetMapping("/sso/config")
    ResponseEntity<SsoConfigResponse> ssoConfig();


    @GetMapping("/oidc/authorize")
    ResponseEntity<OidcAuthorizeResponse> oidcAuthorize(
            @RequestParam(value = "redirectUri", required = false) String redirectUri
    );


    @PostMapping("/oidc/callback")
    ResponseEntity<AuthTokenResponse> oidcCallback(
            @Valid @RequestBody OidcCallbackRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );


    @PostMapping("/mfa/verify")
    ResponseEntity<AuthTokenResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );


    @GetMapping("/mfa/status")
    ResponseEntity<MfaStatusResponse> mfaStatus(@AuthenticationPrincipal DavsUser principal);


    @PostMapping("/mfa/setup")
    ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal DavsUser principal);


    @PostMapping("/mfa/enable")
    ResponseEntity<MfaStatusResponse> enableMfa(
            @AuthenticationPrincipal DavsUser principal,
            @Valid @RequestBody MfaCodeRequest request
    );


    @PostMapping("/mfa/disable")
    ResponseEntity<MfaStatusResponse> disableMfa(
            @AuthenticationPrincipal DavsUser principal,
            @Valid @RequestBody MfaCodeRequest request
    );


    public record OidcCallbackRequest(@NotBlank String code, @NotBlank String state, String redirectUri) {
    }


    public record MfaVerifyRequest(@NotBlank String mfaToken, @NotBlank String code) {
    }


    public record MfaCodeRequest(@NotBlank String code) {
    }


    @PostMapping("/accept-terms")
    ResponseEntity<AuthUserDto> acceptTerms(
            @AuthenticationPrincipal DavsUser principal,
            @RequestBody AcceptTermsRequest request
    );


    @PostMapping("/refresh")
    ResponseEntity<AuthTokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );


    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @AuthenticationPrincipal DavsUser principal,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );


    @GetMapping("/me")
    ResponseEntity<AuthUserDto> me(@AuthenticationPrincipal DavsUser principal);
}
