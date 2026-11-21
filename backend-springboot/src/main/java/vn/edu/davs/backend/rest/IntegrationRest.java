package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.IntegrationConnectResponse;
import vn.edu.davs.backend.dto.IntegrationDto;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin-only REST endpoints for UC25 External Platform Integrations ({@code /api/v1/integrations}).
 */
@RequestMapping("/api/v1/integrations")
public interface IntegrationRest {


    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<List<IntegrationDto>> listIntegrations(@AuthenticationPrincipal DavsUser actor);


    @PostMapping("/{platform}/connect")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<IntegrationConnectResponse> initiateConnect(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform
    );


    @GetMapping("/{platform}/callback")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<IntegrationDto> handleCallback(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "accountName", required = false) String accountName,
            @RequestParam(value = "expiresInSeconds", required = false) Long expiresInSeconds
    );


    @DeleteMapping("/{platform}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    ResponseEntity<IntegrationDto> disconnect(
            @AuthenticationPrincipal DavsUser actor,
            @PathVariable String platform
    );
}
