package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.service.SettingsService;
import vn.edu.davs.backend.dto.SystemSettingDto;
import vn.edu.davs.backend.dto.UpdateSettingsRequest;
import vn.edu.davs.backend.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST endpoints for reading and updating {@code system_settings} at runtime (T-008).
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "Quản lý cấu hình hệ thống (chỉ SITE_ADMIN)")
public class AdminSettingsController {

    private final SettingsService settingsService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Operation(summary = "Lấy toàn bộ cấu hình hệ thống")
    public List<SystemSettingDto> getSettings(@AuthenticationPrincipal DavsUser currentUser) {
        permissionService.requireRole(currentUser, SiteRole.SITE_ADMIN);
        return settingsService.listAllSettings();
    }

    @PutMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Operation(summary = "Cập nhật cấu hình hệ thống (có hiệu lực ngay)")
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
