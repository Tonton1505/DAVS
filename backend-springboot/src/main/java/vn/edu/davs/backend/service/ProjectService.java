package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcProjectRepository;
import vn.edu.davs.backend.dao.JdbcWorkbookRepository;
import vn.edu.davs.backend.dto.AddCollectionItemRequest;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.ChangeProjectOwnerRequest;
import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ContentItemDto;
import vn.edu.davs.backend.dto.ContentListResponse;
import vn.edu.davs.backend.dto.CreateCollectionRequest;
import vn.edu.davs.backend.dto.CreateProjectRequest;
import vn.edu.davs.backend.dto.DeleteProjectRequest;
import vn.edu.davs.backend.dto.MoveProjectRequest;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.ProjectTreeNodeDto;
import vn.edu.davs.backend.dto.UpdateProjectRequest;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.PermissionMode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.exception.FieldErrorItem;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC05 &amp; UC06 business logic for creating, browsing, renaming, moving,
 * changing ownership, soft-deleting/restoring projects, and managing personal collections.
 */
@Service
public class ProjectService {

    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_SEARCH_QUERY_LENGTH = 100;
    private static final Pattern FORBIDDEN_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private final JdbcProjectRepository projectRepository;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final PermissionService permissionService;
    private final JdbcWorkbookRepository workbookRepository;

    public ProjectService(
            JdbcProjectRepository projectRepository,
            SettingsService settingsService,
            AuditService auditService
    ) {
        this(projectRepository, settingsService, auditService, null, null);
    }

    @Autowired
    public ProjectService(
            JdbcProjectRepository projectRepository,
            SettingsService settingsService,
            AuditService auditService,
            PermissionService permissionService,
            JdbcWorkbookRepository workbookRepository
    ) {
        this.projectRepository = projectRepository;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.permissionService = permissionService;
        this.workbookRepository = workbookRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> listProjects() {
        return projectRepository.listActiveProjects();
    }

    @Transactional(readOnly = true)
    public ProjectDto getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    @Transactional(readOnly = true)
    public List<ProjectTreeNodeDto> getProjectTree(DavsUser actor) {
        List<ProjectDto> allProjects = projectRepository.listActiveProjects();
        List<ProjectDto> visible = allProjects.stream()
                .filter(p -> canViewProject(actor, p))
                .toList();
        Map<UUID, List<ProjectDto>> byParent = visible.stream()
                .filter(p -> p.parentId() != null)
                .collect(Collectors.groupingBy(ProjectDto::parentId));
        Set<UUID> visibleIds = visible.stream().map(ProjectDto::id).collect(Collectors.toSet());
        List<ProjectDto> roots = visible.stream()
                .filter(p -> p.parentId() == null || !visibleIds.contains(p.parentId()))
                .toList();
        return roots.stream()
                .map(root -> buildTreeNode(root, byParent))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentListResponse getContent(UUID projectId, String query, String sort, DavsUser actor) {
        String trimmedQuery = query != null ? query.trim() : "";
        if (trimmedQuery.length() > MAX_SEARCH_QUERY_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("q", "Từ khóa tìm kiếm tối đa 100 ký tự"))
            );
        }

        ProjectDto currentProject = null;
        List<ProjectDto> breadcrumbs = new ArrayList<>();
        if (projectId != null) {
            currentProject = projectRepository.findById(projectId)
                    .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
            if (!canViewProject(actor, currentProject)) {
                throw new DavsException(ErrorCode.ERR_PRJ_03);
            }
            breadcrumbs = buildBreadcrumbs(currentProject);
        }

        List<ContentItemDto> items = new ArrayList<>();
        List<ProjectDto> allProjects = projectRepository.listActiveProjects();
        for (ProjectDto proj : allProjects) {
            boolean matchesParent = trimmedQuery.isEmpty()
                    ? (projectId == null ? proj.parentId() == null : projectId.equals(proj.parentId()))
                    : (projectId == null || projectId.equals(proj.parentId()) || projectId.equals(proj.id()));
            if (!matchesParent) {
                continue;
            }
            if (!trimmedQuery.isEmpty() && !matchesQuery(proj.name(), proj.description(), trimmedQuery)) {
                continue;
            }
            if (!canViewProject(actor, proj)) {
                continue;
            }
            String ownerName = projectRepository.findOwnerDisplayName(proj.ownerId());
            int childCount = projectRepository.countChildItems(proj.id());
            List<String> actions = computeProjectAllowedActions(actor, proj);
            items.add(new ContentItemDto(
                    proj.id(),
                    "PROJECT",
                    proj.name(),
                    proj.description(),
                    proj.parentId(),
                    proj.ownerId(),
                    ownerName,
                    proj.permissionMode(),
                    proj.isDefault(),
                    1,
                    childCount,
                    actions,
                    proj.updatedAt()
            ));
        }

        if (workbookRepository != null) {
            List<JdbcWorkbookRepository.WorkbookRow> workbookRows = projectId != null && trimmedQuery.isEmpty()
                    ? workbookRepository.listByProject(projectId)
                    : workbookRepository.listAllActive();
            for (JdbcWorkbookRepository.WorkbookRow wb : workbookRows) {
                if (projectId != null && !projectId.equals(wb.projectId())) {
                    continue;
                }
                if (!trimmedQuery.isEmpty() && !matchesQuery(wb.name(), wb.description(), trimmedQuery)) {
                    continue;
                }
                if (!canViewWorkbook(actor, wb)) {
                    continue;
                }
                List<String> wbActions = computeWorkbookAllowedActions(actor, wb);
                items.add(new ContentItemDto(
                        wb.id(),
                        "WORKBOOK",
                        wb.name(),
                        wb.description(),
                        wb.projectId(),
                        wb.ownerId(),
                        wb.ownerName(),
                        "INHERIT",
                        false,
                        wb.currentVersion(),
                        0,
                        wbActions,
                        wb.updatedAt()
                ));
            }
        }

        items.sort(resolveComparator(sort));
        return new ContentListResponse(
                currentProject != null ? currentProject.id() : null,
                currentProject != null ? currentProject.name() : "Tất cả dự án",
                breadcrumbs,
                items,
                items.size()
        );
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, DavsUser actor) {
        if (actor == null || !actor.getSiteRole().isAtLeast(SiteRole.CREATOR)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        String trimmedName = validateProjectName(request != null ? request.name() : null);
        String description = normalizeDescription(request != null ? request.description() : null);

        UUID parentId = request.parentId();
        int maxDepth = settingsService.getInt(SettingKey.PROJECT_MAX_DEPTH);
        int targetDepth = 1;
        if (parentId != null) {
            int parentDepth = projectRepository.computeDepth(parentId);
            if (parentDepth == 0) {
                throw new DavsException(
                        ErrorCode.ERR_PRJ_02,
                        List.of(new FieldErrorItem("parentId", "Thư mục cha không tồn tại"))
                );
            }
            targetDepth = parentDepth + 1;
            if (targetDepth > maxDepth) {
                throw new DavsException(
                        ErrorCode.ERR_PRJ_02,
                        List.of(new FieldErrorItem("parentId", "Vượt quá độ sâu phân cấp tối đa " + maxDepth + " cấp"))
                );
            }
        }

        if (projectRepository.existsSiblingName(parentId, trimmedName)) {
            throw new DavsException(ErrorCode.ERR_PRJ_01);
        }

        PermissionMode requestedMode = request.permissionMode() != null
                ? request.permissionMode()
                : PermissionMode.CUSTOM;
        String dbPermissionMode = requestedMode == PermissionMode.LOCKED ? "LOCKED" : "CUSTOMIZABLE";

        UUID projectId = projectRepository.insertProject(
                trimmedName,
                description,
                parentId,
                actor.getId(),
                dbPermissionMode
        );

        if (requestedMode == PermissionMode.INHERIT && parentId != null) {
            projectRepository.copyParentPermissionRules(parentId, projectId);
        }

        ProjectDto created = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found after insert: " + projectId));

        auditService.log(AuditEvent.of(
                "PROJECT_CREATE",
                actor.getId(),
                actor.getEmail(),
                "PROJECT",
                projectId.toString()
        ).withStates(null, Map.of(
                "name", created.name(),
                "parentId", created.parentId() != null ? created.parentId().toString() : "",
                "permissionMode", requestedMode.name(),
                "depth", targetDepth
        )));

        return created;
    }

    @Transactional
    public ProjectDto renameProject(UUID id, UpdateProjectRequest request, DavsUser actor) {
        ProjectDto existing = getProject(id);
        requireManagePermission(actor, existing);

        String trimmedName = validateProjectName(request != null ? request.name() : null);
        String description = request != null && request.description() != null
                ? normalizeDescription(request.description())
                : existing.description();

        if (projectRepository.existsSiblingNameExcluding(existing.parentId(), trimmedName, id)) {
            throw new DavsException(ErrorCode.ERR_PRJ_01);
        }

        projectRepository.updateProject(id, trimmedName, description);
        ProjectDto updated = getProject(id);

        auditService.log(AuditEvent.of(
                "PROJECT_RENAME",
                actor.getId(),
                actor.getEmail(),
                "PROJECT",
                id.toString()
        ).withStates(
                Map.of("name", existing.name(), "description", existing.description() != null ? existing.description() : ""),
                Map.of("name", updated.name(), "description", updated.description() != null ? updated.description() : "")
        ));

        return updated;
    }

    @Transactional
    public ProjectDto moveProject(UUID id, MoveProjectRequest request, DavsUser actor) {
        ProjectDto existing = getProject(id);
        if (existing.isDefault()) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        requireManagePermission(actor, existing);

        UUID targetId = request != null ? request.targetId() : null;
        if (targetId != null && (targetId.equals(id) || projectRepository.isDescendantOf(targetId, id))) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("targetId", "Không thể di chuyển dự án vào chính nó hoặc dự án con của nó"))
            );
        }

        ProjectDto targetProject = null;
        int targetParentDepth = 0;
        if (targetId != null) {
            targetProject = projectRepository.findById(targetId)
                    .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
            if (!canPublishToProject(actor, targetProject)) {
                throw new DavsException(ErrorCode.ERR_PRJ_03);
            }
            targetParentDepth = projectRepository.computeDepth(targetId);
        }

        int maxDepth = settingsService.getInt(SettingKey.PROJECT_MAX_DEPTH);
        int subtreeHeight = projectRepository.computeSubtreeHeight(id);
        if (targetParentDepth + subtreeHeight > maxDepth) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("targetId", "Vượt quá độ sâu phân cấp tối đa " + maxDepth + " cấp"))
            );
        }

        if (projectRepository.existsSiblingNameExcluding(targetId, existing.name(), id)) {
            throw new DavsException(ErrorCode.ERR_PRJ_01);
        }

        projectRepository.moveProject(id, targetId);
        if (targetProject != null && "LOCKED".equalsIgnoreCase(targetProject.permissionMode())) {
            projectRepository.replaceProjectRulesFromParent(targetId, id);
        }

        ProjectDto moved = getProject(id);
        auditService.log(AuditEvent.of(
                "PROJECT_MOVE",
                actor.getId(),
                actor.getEmail(),
                "PROJECT",
                id.toString()
        ).withStates(
                Map.of("parentId", existing.parentId() != null ? existing.parentId().toString() : "ROOT"),
                Map.of("parentId", moved.parentId() != null ? moved.parentId().toString() : "ROOT")
        ));
        return moved;
    }

    @Transactional
    public ProjectDto changeOwner(UUID id, ChangeProjectOwnerRequest request, DavsUser actor) {
        ProjectDto existing = getProject(id);
        if (!canChangeOwnerOrPermissions(actor, existing)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        if (request == null || request.userId() == null) {
            throw new DavsException(ErrorCode.ERR_USR_06);
        }

        JdbcProjectRepository.OwnerCandidateRow candidate = projectRepository.findUserForOwnership(request.userId())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_USR_06));
        if (!"ACTIVE".equalsIgnoreCase(candidate.status())
                || !candidate.siteRole().isAtLeast(SiteRole.CREATOR)) {
            throw new DavsException(ErrorCode.ERR_USR_06);
        }

        projectRepository.updateOwner(id, candidate.id());
        ProjectDto updated = getProject(id);

        auditService.log(AuditEvent.of(
                "PROJECT_CHANGE_OWNER",
                actor.getId(),
                actor.getEmail(),
                "PROJECT",
                id.toString()
        ).withStates(
                Map.of("ownerId", existing.ownerId() != null ? existing.ownerId().toString() : ""),
                Map.of("ownerId", candidate.id().toString(), "ownerEmail", candidate.email())
        ));
        return updated;
    }

    @Transactional
    public void deleteProject(UUID id, DeleteProjectRequest request, DavsUser actor) {
        ProjectDto existing = getProject(id);
        if (existing.isDefault() || (existing.parentId() == null && "Default".equalsIgnoreCase(existing.name()))) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        if (!canDeleteProject(actor, existing)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        String confirmName = request != null && request.confirmName() != null ? request.confirmName().trim() : "";
        if (!existing.name().equals(confirmName)) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("confirmName", "Tên xác nhận phải trùng khớp với tên dự án"))
            );
        }

        projectRepository.softDeleteProject(id);
        auditService.log(AuditEvent.of(
                "PROJECT_DELETE",
                actor.getId(),
                actor.getEmail(),
                "PROJECT",
                id.toString()
        ).withStates(Map.of("name", existing.name(), "deleted", false), Map.of("deleted", true)));
    }

    @Transactional
    public ProjectDto restoreProject(UUID id, DavsUser actor) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        JdbcProjectRepository.DeletedProjectRow deletedRow = projectRepository.findDeletedById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        ProjectDto project = deletedRow.project();
        if (actor.getSiteRole() != SiteRole.SITE_ADMIN
                && (project.ownerId() == null || !project.ownerId().equals(actor.getId()))) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        int retentionDays = settingsService.getInt(SettingKey.TRASH_RETENTION_DAYS);
        if (deletedRow.deletedAt() != null
                && Duration.between(deletedRow.deletedAt(), Instant.now()).toDays() > retentionDays) {
            throw new DavsException(ErrorCode.ERR_PRJ_09);
        }

        projectRepository.restoreProject(id);
        ProjectDto restored = getProject(id);
        auditService.log(AuditEvent.of(
                "PROJECT_RESTORE",
                actor.getId(),
                actor.getEmail(),
                "PROJECT",
                id.toString()
        ).withStates(Map.of("deleted", true), Map.of("deleted", false, "name", restored.name())));
        return restored;
    }

    @Transactional(readOnly = true)
    public List<CollectionDto> listCollections(DavsUser actor) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        return projectRepository.listCollectionsByOwner(actor.getId());
    }

    @Transactional
    public CollectionDto createCollection(CreateCollectionRequest request, DavsUser actor) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        String name = request != null && request.name() != null ? request.name().trim() : "";
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new DavsException(ErrorCode.ERR_PRJ_02);
        }
        String description = normalizeDescription(request.description());
        UUID id = projectRepository.insertCollection(name, description, actor.getId());
        return projectRepository.findCollectionById(id)
                .orElseThrow(() -> new IllegalStateException("Collection not found after insert: " + id));
    }

    @Transactional
    public CollectionDto addCollectionItem(UUID collectionId, AddCollectionItemRequest request, DavsUser actor) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        CollectionDto collection = projectRepository.findCollectionById(collectionId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        if (!actor.getId().equals(collection.ownerId()) && actor.getSiteRole() != SiteRole.SITE_ADMIN) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        String type = request != null && request.resourceType() != null
                ? request.resourceType().trim().toUpperCase(Locale.ROOT)
                : "WORKBOOK";
        projectRepository.addCollectionItem(collectionId, type, request.resourceId());
        return projectRepository.findCollectionById(collectionId)
                .orElseThrow(() -> new IllegalStateException("Collection not found: " + collectionId));
    }

    public boolean canPublishToProject(DavsUser actor, ProjectDto project) {
        if (actor == null || project == null || !actor.getSiteRole().supports(Capability.PUBLISH)) {
            return false;
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (project.ownerId() != null && project.ownerId().equals(actor.getId()))
                || project.isDefault()) {
            return true;
        }
        return permissionService != null
                && permissionService.can(actor, Capability.PUBLISH, ResourceRef.project(project.id()));
    }

    public boolean canViewProject(DavsUser actor, ProjectDto project) {
        if (actor == null || project == null) {
            return false;
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (project.ownerId() != null && project.ownerId().equals(actor.getId()))
                || project.isDefault()) {
            return true;
        }
        return permissionService == null
                || permissionService.can(actor, Capability.VIEW, ResourceRef.project(project.id()));
    }

    private boolean canViewWorkbook(DavsUser actor, JdbcWorkbookRepository.WorkbookRow wb) {
        if (actor == null || wb == null) {
            return false;
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (wb.ownerId() != null && wb.ownerId().equals(actor.getId()))) {
            return true;
        }
        return permissionService == null
                || permissionService.can(actor, Capability.VIEW, ResourceRef.workbook(wb.id()));
    }

    private void requireManagePermission(DavsUser actor, ProjectDto project) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (project.ownerId() != null
                && project.ownerId().equals(actor.getId())
                && actor.getSiteRole().isAtLeast(SiteRole.CREATOR))) {
            return;
        }
        if (permissionService != null
                && (permissionService.can(actor, Capability.SAVE, ResourceRef.project(project.id()))
                || permissionService.can(actor, Capability.CHANGE_PERMISSIONS, ResourceRef.project(project.id())))) {
            return;
        }
        throw new DavsException(ErrorCode.ERR_PRJ_03);
    }

    private boolean canChangeOwnerOrPermissions(DavsUser actor, ProjectDto project) {
        if (actor == null) {
            return false;
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (project.ownerId() != null
                && project.ownerId().equals(actor.getId())
                && actor.getSiteRole().isAtLeast(SiteRole.CREATOR))) {
            return true;
        }
        return permissionService != null
                && permissionService.can(actor, Capability.CHANGE_PERMISSIONS, ResourceRef.project(project.id()));
    }

    private boolean canDeleteProject(DavsUser actor, ProjectDto project) {
        if (actor == null || project.isDefault()) {
            return false;
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (project.ownerId() != null
                && project.ownerId().equals(actor.getId())
                && actor.getSiteRole().isAtLeast(SiteRole.CREATOR))) {
            return true;
        }
        return permissionService != null
                && permissionService.can(actor, Capability.DELETE, ResourceRef.project(project.id()));
    }

    private List<String> computeProjectAllowedActions(DavsUser actor, ProjectDto project) {
        List<String> actions = new ArrayList<>();
        actions.add("VIEW");
        actions.add("ADD_TO_COLLECTION");
        if (canPublishToProject(actor, project)) {
            actions.add("UPLOAD_WORKBOOK");
        }
        boolean canManage = actor != null && (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (project.ownerId() != null
                && project.ownerId().equals(actor.getId())
                && actor.getSiteRole().isAtLeast(SiteRole.CREATOR))
                || (permissionService != null
                && permissionService.can(actor, Capability.SAVE, ResourceRef.project(project.id()))));
        if (canManage) {
            actions.add("RENAME");
            if (!project.isDefault()) {
                actions.add("MOVE");
            }
        }
        if (canChangeOwnerOrPermissions(actor, project)) {
            actions.add("CHANGE_OWNER");
            actions.add("CHANGE_PERMISSIONS");
        }
        if (canDeleteProject(actor, project)) {
            actions.add("DELETE");
        }
        return actions;
    }

    private List<String> computeWorkbookAllowedActions(DavsUser actor, JdbcWorkbookRepository.WorkbookRow wb) {
        List<String> actions = new ArrayList<>();
        actions.add("VIEW");
        actions.add("ADD_TO_COLLECTION");
        actions.add("VERSION_HISTORY");
        boolean isOwnerOrAdmin = actor != null && (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (wb.ownerId() != null && wb.ownerId().equals(actor.getId())));
        if (isOwnerOrAdmin || (permissionService != null
                && permissionService.can(actor, Capability.DOWNLOAD, ResourceRef.workbook(wb.id())))) {
            actions.add("DOWNLOAD");
        }
        if (isOwnerOrAdmin || (permissionService != null
                && permissionService.can(actor, Capability.DELETE, ResourceRef.workbook(wb.id())))) {
            actions.add("DELETE");
        }
        return actions;
    }

    private ProjectTreeNodeDto buildTreeNode(ProjectDto node, Map<UUID, List<ProjectDto>> byParent) {
        List<ProjectTreeNodeDto> children = byParent.getOrDefault(node.id(), List.of())
                .stream()
                .map(child -> buildTreeNode(child, byParent))
                .toList();
        return new ProjectTreeNodeDto(
                node.id(),
                node.name(),
                node.description(),
                node.parentId(),
                node.ownerId(),
                node.permissionMode(),
                node.isDefault(),
                node.depth(),
                children
        );
    }

    private List<ProjectDto> buildBreadcrumbs(ProjectDto current) {
        LinkedList<ProjectDto> chain = new LinkedList<>();
        ProjectDto cursor = current;
        int guard = 0;
        while (cursor != null && guard++ < 10) {
            chain.addFirst(cursor);
            cursor = cursor.parentId() != null
                    ? projectRepository.findById(cursor.parentId()).orElse(null)
                    : null;
        }
        return chain;
    }

    private static boolean matchesQuery(String name, String description, String query) {
        String lowerQ = query.toLowerCase(Locale.ROOT);
        boolean nameMatch = name != null && name.toLowerCase(Locale.ROOT).contains(lowerQ);
        boolean descMatch = description != null && description.toLowerCase(Locale.ROOT).contains(lowerQ);
        return nameMatch || descMatch;
    }

    private static Comparator<ContentItemDto> resolveComparator(String sort) {
        if ("name".equalsIgnoreCase(sort)) {
            return Comparator.comparing((ContentItemDto i) -> !"PROJECT".equals(i.resourceType()))
                    .thenComparing(i -> i.name() != null ? i.name().toLowerCase(Locale.ROOT) : "");
        }
        if ("owner".equalsIgnoreCase(sort)) {
            return Comparator.comparing((ContentItemDto i) -> !"PROJECT".equals(i.resourceType()))
                    .thenComparing(i -> i.ownerName() != null ? i.ownerName().toLowerCase(Locale.ROOT) : "");
        }
        return Comparator.comparing((ContentItemDto i) -> !"PROJECT".equals(i.resourceType()))
                .thenComparing(ContentItemDto::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static String validateProjectName(String rawName) {
        String trimmedName = rawName != null ? rawName.trim() : "";
        if (trimmedName.isEmpty()
                || trimmedName.length() > MAX_NAME_LENGTH
                || FORBIDDEN_NAME_CHARS.matcher(trimmedName).find()) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("name", ErrorCode.ERR_PRJ_02.getMessageTemplate()))
            );
        }
        return trimmedName;
    }

    private static String normalizeDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return null;
        }
        String trimmed = rawDescription.trim();
        if (trimmed.length() > 500) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("description", "Mô tả tối đa 500 ký tự"))
            );
        }
        return trimmed;
    }
}
