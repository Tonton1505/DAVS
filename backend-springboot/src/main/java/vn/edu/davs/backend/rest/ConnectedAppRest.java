package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.ConnectedAppDto;
import vn.edu.davs.backend.dto.ConnectedAppTokenRequest;
import vn.edu.davs.backend.dto.ConnectedAppTokenResponse;
import vn.edu.davs.backend.dto.CreateConnectedAppRequest;
import vn.edu.davs.backend.dto.UpdateConnectedAppRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST controller for UC26: Connected Applications Management (FR-INT-01, URD-18, SCR-26).
 * Handles Direct Trust, OAuth 2.0 Trust, secret rotation, and token invalidation.
 */
@RequestMapping("/api/v1/connected-apps")
public interface ConnectedAppRest {


    @PostMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<ConnectedAppDto> createConnectedApp(
            @RequestBody CreateConnectedAppRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<ConnectedAppDto>> listConnectedApps(
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<ConnectedAppDto> getConnectedApp(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<ConnectedAppDto> rotateSecret(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<ConnectedAppDto> updateConnectedApp(
            @PathVariable("id") UUID id,
            @RequestBody UpdateConnectedAppRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<Void> deleteConnectedApp(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @PostMapping("/token")
    ResponseEntity<ConnectedAppTokenResponse> generateToken(
            @RequestBody ConnectedAppTokenRequest request
    );
}
