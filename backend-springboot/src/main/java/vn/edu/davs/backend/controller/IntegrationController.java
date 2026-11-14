package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.IntegrationConnectResponse;
import vn.edu.davs.backend.dto.IntegrationDto;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.IntegrationService;
import vn.edu.davs.backend.service.PermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST endpoints for UC25 External Platform Integrations ({@code /api/v1/integrations}).
 */
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;
    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<List<IntegrationDto>> listIntegrations(@AuthenticationPrincipal DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.listIntegrations());
    }

    @PostMapping("/{platform}/connect")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<IntegrationConnectResponse> initiateConnect(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.initiateConnect(platform, actor));
    }

    @GetMapping("/{platform}/callback")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<IntegrationDto> handleCallback(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "accountName", required = false) String accountName,
            @RequestParam(value = "expiresInSeconds", required = false) Long expiresInSeconds
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.handleCallback(
                platform, code, state, error, accountName, expiresInSeconds, actor
        ));
    }

    @DeleteMapping("/{platform}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<IntegrationDto> disconnect(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.disconnect(platform, actor));
    }
}
