package vn.edu.davs.backend.project;

import vn.edu.davs.backend.dao.JdbcProjectRepository;
import vn.edu.davs.backend.dao.JdbcWorkbookRepository;
import vn.edu.davs.backend.dto.AddCollectionItemRequest;
import vn.edu.davs.backend.dto.ChangeProjectOwnerRequest;
import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ContentListResponse;
import vn.edu.davs.backend.dto.CreateCollectionRequest;
import vn.edu.davs.backend.dto.DeleteProjectRequest;
import vn.edu.davs.backend.dto.MoveProjectRequest;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.ProjectTreeNodeDto;
import vn.edu.davs.backend.dto.UpdateProjectRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.ProjectService;
import vn.edu.davs.backend.service.SettingsService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UC06 Project Management, Content Browsing, and Collections.
 */
class ProjectManagementTests {

    private JdbcProjectRepository projectRepository;
    private SettingsService settingsService;
    private AuditService auditService;
    private PermissionService permissionService;
    private JdbcWorkbookRepository workbookRepository;
    private ProjectService projectService;

    private UUID creatorId;
    private DavsUser creator;
    private DavsUser viewer;

    @BeforeEach
    void setUp() {
        projectRepository = mock(JdbcProjectRepository.class);
        settingsService = mock(SettingsService.class);
        auditService = mock(AuditService.class);
        permissionService = mock(PermissionService.class);
        workbookRepository = mock(JdbcWorkbookRepository.class);

        when(settingsService.getInt(SettingKey.PROJECT_MAX_DEPTH)).thenReturn(5);
        when(settingsService.getInt(SettingKey.TRASH_RETENTION_DAYS)).thenReturn(30);

        projectService = new ProjectService(
                projectRepository,
                settingsService,
                auditService,
                permissionService,
                workbookRepository
        );

        creatorId = UUID.randomUUID();
        creator = DavsUser.of(creatorId, "creator@davs.local", "Creator User", SiteRole.CREATOR);
        viewer = DavsUser.of(UUID.randomUUID(), "viewer@davs.local", "Viewer User", SiteRole.VIEWER);
    }

    @Test
    void uc06RenameProjectValidatesDuplicateAndForbiddenChars() {
        UUID projectId = UUID.randomUUID();
        ProjectDto existing = new ProjectDto(
                projectId,
                "Sales 2025",
                "Old desc",
                null,
                creatorId,
                "CUSTOMIZABLE",
                false,
                1,
                Instant.now(),
                Instant.now()
        );
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));

        // Forbidden character -> ERR-PRJ-02
        assertThatThrownBy(() -> projectService.renameProject(
                projectId,
                new UpdateProjectRequest("Sales/2026", "Desc"),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // Duplicate sibling name -> ERR-PRJ-01
        when(projectRepository.existsSiblingNameExcluding(null, "Marketing", projectId)).thenReturn(true);
        assertThatThrownBy(() -> projectService.renameProject(
                projectId,
                new UpdateProjectRequest("Marketing", "Desc"),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_01);

        // Unauthorized viewer -> ERR-PRJ-03
        assertThatThrownBy(() -> projectService.renameProject(
                projectId,
                new UpdateProjectRequest("Sales 2026", "Desc"),
                viewer
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);

        // Valid rename -> updates and logs audit
        when(projectRepository.existsSiblingNameExcluding(null, "Sales 2026", projectId)).thenReturn(false);
        ProjectDto renamed = new ProjectDto(
                projectId,
                "Sales 2026",
                "Updated desc",
                null,
                creatorId,
                "CUSTOMIZABLE",
                false,
                1,
                Instant.now(),
                Instant.now()
        );
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(renamed));

        ProjectDto result = projectService.renameProject(
                projectId,
                new UpdateProjectRequest("Sales 2026", "Updated desc"),
                creator
        );
        assertThat(result.name()).isEqualTo("Sales 2026");
        verify(projectRepository).updateProject(projectId, "Sales 2026", "Updated desc");
        verify(auditService).log(any());
    }

    @Test
    void uc06MoveProjectPreventsCycleAndEnforcesMaxDepthAndLockedTarget() {
        UUID sourceId = UUID.randomUUID();
        UUID descendantId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        ProjectDto source = new ProjectDto(
                sourceId,
                "Analytics",
                "Source",
                null,
                creatorId,
                "CUSTOMIZABLE",
                false,
                1,
                Instant.now(),
                Instant.now()
        );
        ProjectDto lockedTarget = new ProjectDto(
                targetId,
                "Finance Root",
                "Locked parent",
                null,
                creatorId,
                "LOCKED",
                false,
                1,
                Instant.now(),
                Instant.now()
        );

        when(projectRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(projectRepository.isDescendantOf(descendantId, sourceId)).thenReturn(true);

        // [6a] Cannot move into itself or descendant -> ERR-PRJ-02
        assertThatThrownBy(() -> projectService.moveProject(
                sourceId,
                new MoveProjectRequest(descendantId),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // [6a] Exceeding max depth 5 -> ERR-PRJ-02
        when(projectRepository.findById(targetId)).thenReturn(Optional.of(lockedTarget));
        when(projectRepository.computeDepth(targetId)).thenReturn(4);
        when(projectRepository.computeSubtreeHeight(sourceId)).thenReturn(2);
        assertThatThrownBy(() -> projectService.moveProject(
                sourceId,
                new MoveProjectRequest(targetId),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // [6a] Valid move into LOCKED target copies parent rules
        when(projectRepository.computeDepth(targetId)).thenReturn(1);
        when(projectRepository.computeSubtreeHeight(sourceId)).thenReturn(1);
        when(projectRepository.existsSiblingNameExcluding(targetId, "Analytics", sourceId)).thenReturn(false);

        projectService.moveProject(sourceId, new MoveProjectRequest(targetId), creator);
        verify(projectRepository).moveProject(sourceId, targetId);
        verify(projectRepository).replaceProjectRulesFromParent(targetId, sourceId);
    }

    @Test
    void uc06ChangeOwnerRequiresActiveCreatorOrAdminElseErrUsr06() {
        UUID projectId = UUID.randomUUID();
        UUID viewerCandidateId = UUID.randomUUID();
        UUID newCreatorId = UUID.randomUUID();

        ProjectDto existing = new ProjectDto(
                projectId,
                "BI Workspace",
                null,
                null,
                creatorId,
                "CUSTOMIZABLE",
                false,
                1,
                Instant.now(),
                Instant.now()
        );
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));

        // [6b] Viewer role cannot own project -> ERR-USR-06
        when(projectRepository.findUserForOwnership(viewerCandidateId)).thenReturn(Optional.of(
                new JdbcProjectRepository.OwnerCandidateRow(
                        viewerCandidateId,
                        "viewer@davs.local",
                        "Viewer",
                        SiteRole.VIEWER,
                        "ACTIVE"
                )
        ));
        assertThatThrownBy(() -> projectService.changeOwner(
                projectId,
                new ChangeProjectOwnerRequest(viewerCandidateId),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_USR_06);

        // [6b] Valid transfer to active Creator
        when(projectRepository.findUserForOwnership(newCreatorId)).thenReturn(Optional.of(
                new JdbcProjectRepository.OwnerCandidateRow(
                        newCreatorId,
                        "newcreator@davs.local",
                        "New Creator",
                        SiteRole.CREATOR,
                        "ACTIVE"
                )
        ));
        projectService.changeOwner(projectId, new ChangeProjectOwnerRequest(newCreatorId), creator);
        verify(projectRepository).updateOwner(projectId, newCreatorId);
    }

    @Test
    void uc06DeleteProjectBlocksDefaultProjectAndRequiresMatchingConfirmName() {
        UUID defaultId = UUID.randomUUID();
        UUID normalId = UUID.randomUUID();

        ProjectDto defaultProject = new ProjectDto(
                defaultId,
                "Default",
                "Default workspace",
                null,
                creatorId,
                "CUSTOMIZABLE",
                true,
                1,
                Instant.now(),
                Instant.now()
        );
        ProjectDto normalProject = new ProjectDto(
                normalId,
                "Q4 Reports",
                "Quarterly",
                null,
                creatorId,
                "CUSTOMIZABLE",
                false,
                1,
                Instant.now(),
                Instant.now()
        );

        when(projectRepository.findById(defaultId)).thenReturn(Optional.of(defaultProject));
        when(projectRepository.findById(normalId)).thenReturn(Optional.of(normalProject));

        // BUS-PRJ-06: Default project cannot be deleted -> ERR-PRJ-03
        assertThatThrownBy(() -> projectService.deleteProject(
                defaultId,
                new DeleteProjectRequest("Default"),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);

        // VAL-PRJ-09: Confirm name mismatch -> ERR-PRJ-02
        assertThatThrownBy(() -> projectService.deleteProject(
                normalId,
                new DeleteProjectRequest("Wrong Name"),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // Valid soft-delete and restore
        projectService.deleteProject(normalId, new DeleteProjectRequest("Q4 Reports"), creator);
        verify(projectRepository).softDeleteProject(normalId);

        when(projectRepository.findDeletedById(normalId)).thenReturn(Optional.of(
                new JdbcProjectRepository.DeletedProjectRow(normalProject, Instant.now())
        ));
        ProjectDto restored = projectService.restoreProject(normalId, creator);
        assertThat(restored.name()).isEqualTo("Q4 Reports");
        verify(projectRepository).restoreProject(normalId);
    }

    @Test
    void uc06ContentTreeAndCollectionsFlow() {
        UUID rootId = UUID.randomUUID();
        ProjectDto root = new ProjectDto(
                rootId,
                "Default",
                "Default root",
                null,
                creatorId,
                "CUSTOMIZABLE",
                true,
                1,
                Instant.now(),
                Instant.now()
        );
        when(projectRepository.listActiveProjects()).thenReturn(List.of(root));
        when(projectRepository.findOwnerDisplayName(creatorId)).thenReturn("Creator User");
        when(projectRepository.countChildItems(rootId)).thenReturn(2);
        when(workbookRepository.listAllActive()).thenReturn(List.of());

        List<ProjectTreeNodeDto> tree = projectService.getProjectTree(creator);
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).name()).isEqualTo("Default");

        ContentListResponse content = projectService.getContent(null, "", "updatedAt", creator);
        assertThat(content.items()).hasSize(1);
        assertThat(content.items().get(0).allowedActions()).doesNotContain("DELETE");

        UUID colId = UUID.randomUUID();
        CollectionDto colDto = new CollectionDto(
                colId,
                "Yêu thích",
                "Bộ sưu tập cá nhân",
                creatorId,
                1,
                List.of(),
                Instant.now(),
                Instant.now()
        );
        when(projectRepository.insertCollection("Yêu thích", "Bộ sưu tập cá nhân", creatorId))
                .thenReturn(colId);
        when(projectRepository.findCollectionById(colId)).thenReturn(Optional.of(colDto));

        CollectionDto createdCol = projectService.createCollection(
                new CreateCollectionRequest("Yêu thích", "Bộ sưu tập cá nhân"),
                creator
        );
        assertThat(createdCol.name()).isEqualTo("Yêu thích");

        projectService.addCollectionItem(
                colId,
                new AddCollectionItemRequest("PROJECT", rootId),
                creator
        );
        verify(projectRepository).addCollectionItem(colId, "PROJECT", rootId);
    }
}
