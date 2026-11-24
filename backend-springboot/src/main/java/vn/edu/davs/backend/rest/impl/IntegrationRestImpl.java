package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.IntegrationConnectResponse;
import vn.edu.davs.backend.dto.IntegrationDto;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.IntegrationService;
import vn.edu.davs.backend.service.PermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.IntegrationRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationRestImpl implements IntegrationRest {


    private final IntegrationService integrationService;

    private final PermissionService permissionService;

    @Override
    public ResponseEntity<List<IntegrationDto>> listIntegrations(@AuthenticationPrincipal DavsUser actor) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.listIntegrations());
    }

    @Override
    public ResponseEntity<IntegrationConnectResponse> initiateConnect(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.initiateConnect(platform, actor));
    }

    @Override
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

    @Override
    public ResponseEntity<IntegrationDto> disconnect(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform
    ) {
        permissionService.requireRole(actor, SiteRole.SITE_ADMIN);
        return ResponseEntity.ok(integrationService.disconnect(platform, actor));
    }
}
