package vn.edu.davs.backend.rest.impl;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.ConnectedAppRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/connected-apps")
public class ConnectedAppRestImpl implements ConnectedAppRest {


    private final ConnectedAppService connectedAppService;

    @Override
    public ResponseEntity<ConnectedAppDto> createConnectedApp(
            @RequestBody CreateConnectedAppRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        ConnectedAppDto created = connectedAppService.createConnectedApp(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<List<ConnectedAppDto>> listConnectedApps(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.listConnectedApps(actor));
    }

    @Override
    public ResponseEntity<ConnectedAppDto> getConnectedApp(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.getConnectedApp(id, actor));
    }

    @Override
    public ResponseEntity<ConnectedAppDto> rotateSecret(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.rotateSecret(id, actor));
    }

    @Override
    public ResponseEntity<ConnectedAppDto> updateConnectedApp(
            @PathVariable("id") UUID id,
            @RequestBody UpdateConnectedAppRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(connectedAppService.updateConnectedApp(id, request, actor));
    }

    @Override
    public ResponseEntity<Void> deleteConnectedApp(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        connectedAppService.deleteConnectedApp(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ConnectedAppTokenResponse> generateToken(
            @RequestBody ConnectedAppTokenRequest request
    ) {
        return ResponseEntity.ok(connectedAppService.generateToken(request));
    }
}
