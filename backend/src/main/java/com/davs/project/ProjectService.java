package com.davs.project;

import com.davs.audit.AuditEvent;
import com.davs.audit.AuditService;
import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.error.FieldErrorItem;
import com.davs.common.security.DavsUser;
import com.davs.common.security.SiteRole;
import com.davs.common.service.SettingKey;
import com.davs.common.service.SettingsService;
import com.davs.permission.PermissionMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC05 business logic for creating and inspecting project folders.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final int MAX_NAME_LENGTH = 128;
    private static final Pattern FORBIDDEN_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private final JdbcProjectRepository projectRepository;
    private final SettingsService settingsService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProjectDto> listProjects() {
        return projectRepository.listActiveProjects();
    }

    @Transactional(readOnly = true)
    public ProjectDto getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, DavsUser actor) {
        if (actor == null || (actor.getSiteRole() != SiteRole.SITE_ADMIN && actor.getSiteRole() != SiteRole.CREATOR)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        String trimmedName = request.name() == null ? "" : request.name().trim();
        if (trimmedName.isEmpty()
                || trimmedName.length() > MAX_NAME_LENGTH
                || FORBIDDEN_NAME_CHARS.matcher(trimmedName).find()) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("name", ErrorCode.ERR_PRJ_02.getMessageTemplate()))
            );
        }

        String description = request.description() != null ? request.description().trim() : null;
        if (description != null && description.isEmpty()) {
            description = null;
        }

        UUID parentId = request.parentId();
        int maxDepth = Math.max(1, settingsService.getInt(SettingKey.PROJECT_MAX_DEPTH));
        if (parentId != null) {
            int parentDepth = projectRepository.computeDepth(parentId);
            if (parentDepth <= 0) {
                throw new DavsException(ErrorCode.ERR_PRJ_02, List.of(
                        new FieldErrorItem("parentId", "Dự án cha không tồn tại.")
                ));
            }
            // BUS-PRJ-02: maximum 5 nested project levels
            if (parentDepth + 1 > maxDepth) {
                throw new DavsException(ErrorCode.ERR_PRJ_02, List.of(
                        new FieldErrorItem("parentId", "Tối đa " + maxDepth + " cấp dự án lồng nhau.")
                ));
            }
        }

        // BUS-PRJ-01: unique name within the same parent (case-insensitive)
        if (projectRepository.existsSiblingName(parentId, trimmedName)) {
            throw new DavsException(ErrorCode.ERR_PRJ_01);
        }

        PermissionMode mode = request.permissionMode() != null ? request.permissionMode() : PermissionMode.CUSTOM;
        UUID createdId = projectRepository.insertProject(
                trimmedName,
                description,
                parentId,
                actor.getId(),
                mode.name()
        );

        // BUS-PRJ-03: inherit permission rules from parent when mode is INHERIT
        if (mode == PermissionMode.INHERIT && parentId != null) {
            projectRepository.copyParentPermissionRules(parentId, createdId);
        }

        ProjectDto created = projectRepository.findById(createdId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SYS_01, createdId.toString()));

        auditService.log(AuditEvent.of(
                        "PROJECT_CREATE",
                        actor.getId(),
                        actor.getEmail(),
                        "PROJECT",
                        createdId.toString())
                .withStates(null, Map.of(
                        "name", trimmedName,
                        "parentId", parentId != null ? parentId.toString() : "ROOT",
                        "permissionMode", mode.name()
                )));
        return created;
    }
}
