package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.ConnectedAppDto;
import vn.edu.davs.backend.dto.ConnectedAppTokenRequest;
import vn.edu.davs.backend.dto.ConnectedAppTokenResponse;
import vn.edu.davs.backend.dto.CreateConnectedAppRequest;
import vn.edu.davs.backend.dto.UpdateConnectedAppRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ConnectedAppService;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for UC26: Connected Applications Management (FR-INT-01, SCR-26).
 */
@RestController
@RequestMapping("/api/v1/connected-apps")
@RequiredArgsConstructor
public class ConnectedAppController {

    private final ConnectedAppService connectedAppService;

    @PostMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<ConnectedAppDto> createConnectedApp(
            @RequestBody CreateConnectedAppRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        ConnectedAppDto created = connectedAppService.createConnectedApp(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<List<ConnectedAppDto>> listConnectedApps(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.listConnectedApps(actor));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<ConnectedAppDto> getConnectedApp(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.getConnectedApp(id, actor));
    }

    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<ConnectedAppDto> rotateSecret(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.rotateSecret(id, actor));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<ConnectedAppDto> updateConnectedApp(
            @PathVariable("id") UUID id,
            @RequestBody UpdateConnectedAppRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.updateConnectedApp(id, request, actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<Void> deleteConnectedApp(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        connectedAppService.deleteConnectedApp(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/token")
    public ResponseEntity<ConnectedAppTokenResponse> generateToken(
            @RequestBody ConnectedAppTokenRequest request
    ) {
        return ResponseEntity.ok(connectedAppService.generateToken(request));
    }
}
