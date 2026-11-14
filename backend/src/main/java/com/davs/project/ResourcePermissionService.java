package com.davs.project;

import com.davs.audit.AuditEvent;
import com.davs.audit.AuditService;
import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.error.FieldErrorItem;
import com.davs.common.security.DavsUser;
import com.davs.common.security.SiteRole;
import com.davs.permission.Capability;
import com.davs.permission.Effect;
import com.davs.permission.PermissionMode;
import com.davs.permission.PermissionService;
import com.davs.permission.ResourceMetadata;
import com.davs.permission.ResourceMetadataResolver;
import com.davs.permission.ResourceRef;
import com.davs.permission.ResourceType;
import com.davs.permission.SubjectType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC07 service for managing resource permission rules, role-ceiling warnings (BUS-PRJ-08),
 * DENY-over-ALLOW conflict resolution (BUS-PRJ-07), locked-project enforcement (DIS-PRJ-03),
 * and effective permission inspection.
 */
@Service
public class ResourcePermissionService {

    private static final int MAX_PARENT_DEPTH = 5;

    private final JdbcResourcePermissionRepository permissionRepository;
    private final JdbcProjectRepository projectRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final Map<ResourceType, ResourceMetadataResolver> resolvers = new ConcurrentHashMap<>();

    @Autowired
    public ResourcePermissionService(
            JdbcResourcePermissionRepository permissionRepository,
            JdbcProjectRepository projectRepository,
            PermissionService permissionService,
            AuditService auditService,
            List<ResourceMetadataResolver> resolverBeans
    ) {
        this.permissionRepository = permissionRepository;
        this.projectRepository = projectRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
        if (resolverBeans != null) {
            for (ResourceMetadataResolver resolver : resolverBeans) {
                if (resolver != null && resolver.supports() != null) {
                    resolvers.put(resolver.supports(), resolver);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PermissionTemplateDto> listTemplates() {
        return List.of(
                new PermissionTemplateDto(
                        "VIEW",
                        "Xem",
                        "Chỉ xem và bình luận nội dung",
                        List.of(Capability.VIEW, Capability.FILTER, Capability.COMMENT)
                ),
                new PermissionTemplateDto(
                        "INTERACT",
                        "Tương tác",
                        "Xem, tương tác bộ lọc, tải xuống và bình luận",
                        List.of(
                                Capability.VIEW,
                                Capability.INTERACT,
                                Capability.FILTER,
                                Capability.DOWNLOAD,
                                Capability.COMMENT
                        )
                ),
                new PermissionTemplateDto(
                        "EDIT",
                        "Chỉnh sửa",
                        "Tương tác và chỉnh sửa trên web, lưu, chia sẻ",
                        List.of(
                                Capability.VIEW,
                                Capability.INTERACT,
                                Capability.FILTER,
                                Capability.DOWNLOAD,
                                Capability.COMMENT,
                                Capability.WEB_EDIT,
                                Capability.SAVE,
                                Capability.SHARE
                        )
                ),
                new PermissionTemplateDto(
                        "PUBLISH",
                        "Xuất bản",
                        "Toàn quyền quản lý, xuất bản và đổi quyền",
                        List.of(Capability.values())
                )
        );
    }

    @Transactional(readOnly = true)
    public ResourcePermissionsResponse getPermissions(String rawType, UUID resourceId, DavsUser actor) {
        ResourceType type = parseResourceType(rawType);
        ResourceInfo info = resolveResource(type, resourceId);
        ResourceRef ref = new ResourceRef(type, resourceId);
        requireReadable(actor, ref, info);

        boolean canEdit = !info.lockedByParent()
                && permissionService.can(actor, Capability.CHANGE_PERMISSIONS, ref);
        List<PermissionRuleDto> rules = permissionRepository.findRulesByResource(type, resourceId);
        return new ResourcePermissionsResponse(
                type.name(),
                resourceId,
                info.name(),
                info.permissionMode().name(),
                info.lockedByParent(),
                canEdit,
                rules
        );
    }

    @Transactional
    public UpdateResourcePermissionsResponse updatePermissions(
            String rawType,
            UUID resourceId,
            UpdateResourcePermissionsRequest request,
            DavsUser actor
    ) {
        ResourceType type = parseResourceType(rawType);
        ResourceInfo info = resolveResource(type, resourceId);
        ResourceRef ref = new ResourceRef(type, resourceId);

        // [6b] DIS-PRJ-03: Resources inside a LOCKED parent project cannot have custom rules edited
        if (info.lockedByParent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dự án cha đang ở chế độ khóa quyền (LOCKED), không thể chỉnh sửa quyền riêng."
            );
        }

        // [6c] Actor must have CHANGE_PERMISSIONS on the target resource
        permissionService.require(actor, Capability.CHANGE_PERMISSIONS, ref);

        List<UpdateResourcePermissionsRequest.RuleInput> incoming =
                request.rules() == null ? List.of() : request.rules();

        // Validate subjects (VAL-PRJ-10) & deduplicate with BUS-PRJ-07 (DENY overrides ALLOW)
        Map<RuleKey, UpdateResourcePermissionsRequest.RuleInput> deduplicated = new LinkedHashMap<>();
        Map<UUID, JdbcResourcePermissionRepository.UserSubjectRow> userCache = new HashMap<>();

        for (var input : incoming) {
            if (input.subjectType() == SubjectType.USER) {
                var userRow = userCache.computeIfAbsent(input.subjectId(), id ->
                        permissionRepository.findActiveUser(id)
                                .orElseThrow(() -> new DavsException(
                                        ErrorCode.ERR_PRJ_02,
                                        List.of(new FieldErrorItem(
                                                "subjectId",
                                                "Người dùng không tồn tại hoặc đã bị vô hiệu hóa."
                                        ))
                                ))
                );
                userCache.put(input.subjectId(), userRow);
            } else {
                if (!permissionRepository.existsActiveGroup(input.subjectId())) {
                    throw new DavsException(
                            ErrorCode.ERR_PRJ_02,
                            List.of(new FieldErrorItem("subjectId", "Nhóm không tồn tại."))
                    );
                }
            }

            RuleKey key = new RuleKey(input.subjectType(), input.subjectId(), input.capability());
            UpdateResourcePermissionsRequest.RuleInput existing = deduplicated.get(key);
            if (existing == null || input.effect() == Effect.DENY) {
                deduplicated.put(key, input);
            }
        }

        List<UpdateResourcePermissionsRequest.RuleInput> finalRules = List.copyOf(deduplicated.values());
        List<String> warnings = new ArrayList<>();
        for (var rule : finalRules) {
            if (rule.subjectType() == SubjectType.USER && rule.effect() == Effect.ALLOW) {
                var userRow = userCache.get(rule.subjectId());
                if (userRow != null && !userRow.siteRole().supports(rule.capability())) {
                    warnings.add(
                            "Quyền " + rule.capability().name()
                                    + " cho " + userRow.displayName()
                                    + " bị giới hạn bởi vai trò " + userRow.siteRole().name()
                                    + " (BUS-PRJ-08)."
                    );
                }
            }
        }

        List<PermissionRuleDto> before = permissionRepository.findRulesByResource(type, resourceId);
        permissionRepository.replaceRules(type, resourceId, finalRules, actor.getId());
        List<PermissionRuleDto> after = permissionRepository.findRulesByResource(type, resourceId);

        auditService.log(AuditEvent.of(
                        "PERMISSION_UPDATE",
                        actor.getId(),
                        actor.getEmail(),
                        type.name(),
                        resourceId.toString())
                .withStates(before, after));

        ResourcePermissionsResponse updated = new ResourcePermissionsResponse(
                type.name(),
                resourceId,
                info.name(),
                info.permissionMode().name(),
                info.lockedByParent(),
                true,
                after
        );
        return new UpdateResourcePermissionsResponse(updated, warnings);
    }

    @Transactional(readOnly = true)
    public EffectivePermissionsResponse getEffectivePermissions(
            String rawType,
            UUID resourceId,
            UUID targetUserId,
            DavsUser actor
    ) {
        ResourceType type = parseResourceType(rawType);
        ResourceInfo info = resolveResource(type, resourceId);
        ResourceRef ref = new ResourceRef(type, resourceId);
        requireReadable(actor, ref, info);

        var targetUserRow = permissionRepository.findActiveUser(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        DavsUser targetPrincipal = DavsUser.of(
                targetUserRow.id(),
                targetUserRow.email(),
                targetUserRow.displayName(),
                targetUserRow.siteRole()
        );
        Set<Capability> effective = permissionService.effective(targetPrincipal, ref);
        return new EffectivePermissionsResponse(
                targetUserRow.id(),
                targetUserRow.displayName(),
                targetUserRow.siteRole().name(),
                type.name(),
                resourceId,
                info.lockedByParent(),
                effective
        );
    }

    @Transactional(readOnly = true)
    public List<PermissionSubjectDto> listSubjects(String rawType, UUID resourceId, DavsUser actor) {
        ResourceType type = parseResourceType(rawType);
        ResourceInfo info = resolveResource(type, resourceId);
        requireReadable(actor, new ResourceRef(type, resourceId), info);
        return permissionRepository.listActiveSubjects();
    }

    private void requireReadable(DavsUser actor, ResourceRef ref, ResourceInfo info) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || permissionService.can(actor, Capability.CHANGE_PERMISSIONS, ref)
                || permissionService.can(actor, Capability.VIEW, ref)
                || (info.lockedByParent() && actor.getId().equals(info.ownerId()))) {
            return;
        }
        throw new DavsException(ErrorCode.ERR_PRJ_03);
    }

    private ResourceType parseResourceType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại tài nguyên không hợp lệ.");
        }
        try {
            return ResourceType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại tài nguyên không hợp lệ: " + rawType);
        }
    }

    private ResourceInfo resolveResource(ResourceType type, UUID resourceId) {
        if (type == ResourceType.PROJECT) {
            ProjectDto project = projectRepository.findById(resourceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dự án."));
            PermissionMode mode = ProjectResourceMetadataResolver.mapMode(project.permissionMode());
            boolean lockedByParent = hasLockedAncestor(new ResourceRef(ResourceType.PROJECT, project.id()));
            return new ResourceInfo(project.name(), project.ownerId(), mode, lockedByParent);
        }

        ResourceMetadataResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên.");
        }
        ResourceMetadata meta = resolver.resolve(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên."));
        boolean lockedByParent = hasLockedAncestor(meta.resource());
        return new ResourceInfo(type.name(), meta.ownerId(), meta.permissionMode(), lockedByParent);
    }

    private boolean hasLockedAncestor(ResourceRef resource) {
        Set<ResourceRef> visited = new HashSet<>();
        visited.add(resource);
        Optional<ResourceMetadata> current = resolveMetadata(resource);
        for (int i = 0; i < MAX_PARENT_DEPTH && current.isPresent() && current.get().parent() != null; i++) {
            ResourceRef parentRef = current.get().parent();
            if (!visited.add(parentRef)) {
                break;
            }
            Optional<ResourceMetadata> parentMeta = resolveMetadata(parentRef);
            if (parentRef.type() == ResourceType.PROJECT
                    && parentMeta.isPresent()
                    && parentMeta.get().permissionMode() == PermissionMode.LOCKED) {
                return true;
            }
            current = parentMeta;
        }
        return false;
    }

    private Optional<ResourceMetadata> resolveMetadata(ResourceRef resource) {
        ResourceMetadataResolver resolver = resolvers.get(resource.type());
        if (resolver == null) {
            return Optional.empty();
        }
        return resolver.resolve(resource.id());
    }

    private record ResourceInfo(String name, UUID ownerId, PermissionMode permissionMode, boolean lockedByParent) {
    }

    private record RuleKey(SubjectType subjectType, UUID subjectId, Capability capability) {
    }
}
