package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.dto.SystemSettingDto;
import vn.edu.davs.backend.dto.UpdateSettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Admin-only REST endpoints for reading and updating {@code system_settings} at runtime (T-008).
 */
@Tag(name = "Admin Settings", description = "Quản lý cấu hình hệ thống (chỉ SITE_ADMIN)")
public interface AdminSettingsRest {


    @GetMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Operation(summary = "Lấy toàn bộ cấu hình hệ thống")
    List<SystemSettingDto> getSettings(@AuthenticationPrincipal DavsUser currentUser);


    @PutMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Operation(summary = "Cập nhật cấu hình hệ thống (có hiệu lực ngay)")
    List<SystemSettingDto> updateSettings(
            @AuthenticationPrincipal DavsUser currentUser,
            @Valid @RequestBody UpdateSettingsRequest request
    );
}
