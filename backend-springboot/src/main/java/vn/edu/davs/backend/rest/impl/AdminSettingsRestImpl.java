package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.service.SettingsService;
import vn.edu.davs.backend.dto.SystemSettingDto;
import vn.edu.davs.backend.dto.UpdateSettingsRequest;
import vn.edu.davs.backend.service.PermissionService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.AdminSettingsRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminSettingsRestImpl implements AdminSettingsRest {


    private final SettingsService settingsService;

    private final PermissionService permissionService;

    private final AuditService auditService;

    @Override
    public List<SystemSettingDto> getSettings(@AuthenticationPrincipal DavsUser currentUser) {
        permissionService.requireRole(currentUser, SiteRole.SITE_ADMIN);
        return settingsService.listAllSettings();
    }

    @Override
    public List<SystemSettingDto> updateSettings(
            @AuthenticationPrincipal DavsUser currentUser,
            @Valid @RequestBody UpdateSettingsRequest request
    ) {
        permissionService.requireRole(currentUser, SiteRole.SITE_ADMIN);

        Map<String, String> beforeSnapshot = new LinkedHashMap<>();
        for (String key : request.settings().keySet()) {
            settingsService.getSetting(key).ifPresent(val -> beforeSnapshot.put(key, val));
        }

        List<SystemSettingDto> updated = settingsService.updateSettings(
                request.settings(),
                currentUser != null ? currentUser.getId() : null
        );

        auditService.log(
                AuditEvent.of("SYSTEM_SETTINGS_UPDATE", "SYSTEM_SETTINGS", "global")
                        .withStates(beforeSnapshot, request.settings())
        );

        return updated;
    }
}
