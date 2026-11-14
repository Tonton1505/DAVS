package vn.edu.davs.backend.project;

import vn.edu.davs.backend.dao.JdbcProjectRepository;
import vn.edu.davs.backend.dao.JdbcWorkbookRepository;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.RestoreWorkbookVersionRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import vn.edu.davs.backend.enu.Bucket;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.StoredObject;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.ProjectService;
import vn.edu.davs.backend.service.SettingsService;
import vn.edu.davs.backend.service.StorageService;
import vn.edu.davs.backend.service.WorkbookService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UC08 Workbook Upload and UC10 Workbook Version History.
 */
class WorkbookAndVersionTests {

    private JdbcWorkbookRepository workbookRepository;
    private JdbcProjectRepository projectRepository;
    private ProjectService projectService;
    private PermissionService permissionService;
    private StorageService storageService;
    private SettingsService settingsService;
    private AuditService auditService;
    private WorkbookService workbookService;

    private UUID projectId;
    private UUID creatorId;
    private DavsUser creator;
    private DavsUser viewer;
    private ProjectDto project;

    @BeforeEach
    void setUp() {
        workbookRepository = mock(JdbcWorkbookRepository.class);
        projectRepository = mock(JdbcProjectRepository.class);
        projectService = mock(ProjectService.class);
        permissionService = mock(PermissionService.class);
        storageService = mock(StorageService.class);
        settingsService = mock(SettingsService.class);
        auditService = mock(AuditService.class);

        when(settingsService.getInt(SettingKey.WORKBOOK_MAX_VERSIONS)).thenReturn(25);

        workbookService = new WorkbookService(
                workbookRepository,
                projectRepository,
                projectService,
                permissionService,
                storageService,
                settingsService,
                auditService
        );

        projectId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        creator = DavsUser.of(creatorId, "creator@davs.local", "Creator", SiteRole.CREATOR);
        viewer = DavsUser.of(UUID.randomUUID(), "viewer@davs.local", "Viewer", SiteRole.VIEWER);

        project = new ProjectDto(
                projectId,
                "Default",
                "Default project",
                null,
                creatorId,
                "CUSTOMIZABLE",
                true,
                1,
                Instant.now(),
                Instant.now()
        );
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectService.canPublishToProject(creator, project)).thenReturn(true);
        when(projectService.canPublishToProject(viewer, project)).thenReturn(false);
    }

    @Test
    void uc08UploadWorkbookValidatesFormatPermissionDuplicateAndVirusScan() {
        MockMultipartFile invalidExt = new MockMultipartFile(
                "file",
                "report.xlsx",
                "application/octet-stream",
                "<workbook/>".getBytes(StandardCharsets.UTF_8)
        );

        // [5a] Invalid extension -> ERR-PRJ-04
        assertThatThrownBy(() -> workbookService.uploadWorkbook(
                projectId,
                "Sales Report",
                "Desc",
                false,
                null,
                invalidExt,
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_04);

        // Missing PUBLISH permission -> ERR-PRJ-03
        MockMultipartFile validTwb = new MockMultipartFile(
                "file",
                "sales.twb",
                "application/xml",
                "<?xml version='1.0'?><workbook/>".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> workbookService.uploadWorkbook(
                projectId,
                "Sales Report",
                "Desc",
                false,
                null,
                validTwb,
                viewer
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);

        // [7a] Existing workbook without overwrite -> 409 ERR-PRJ-06
        UUID existingWbId = UUID.randomUUID();
        JdbcWorkbookRepository.WorkbookRow existingRow = new JdbcWorkbookRepository.WorkbookRow(
                existingWbId,
                projectId,
                "Default",
                "Sales Report",
                "Old desc",
                1,
                creatorId,
                "Creator",
                "IMPORTED",
                false,
                128L,
                "sales.twb",
                Instant.now(),
                Instant.now()
        );
        when(workbookRepository.findActiveByProjectAndName(projectId, "Sales Report"))
                .thenReturn(Optional.of(existingRow));

        assertThatThrownBy(() -> workbookService.uploadWorkbook(
                projectId,
                "Sales Report",
                "Desc",
                false,
                null,
                validTwb,
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_06);

        // [5c] Virus scan failure -> ERR-PRJ-08 and security audit log
        when(storageService.put(eq(Bucket.WORKBOOKS), anyString(), anyString(), any(), any(), anyLong()))
                .thenThrow(new DavsException(ErrorCode.ERR_PRJ_08));
        assertThatThrownBy(() -> workbookService.uploadWorkbook(
                projectId,
                "Sales Report",
                "Desc",
                true,
                "Overwrite",
                validTwb,
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_08);
        verify(auditService).log(any());
    }

    @Test
    void uc08UploadOverwriteIncrementsVersionAndPrunesOldestExceedingMaxVersions() {
        MockMultipartFile validTwb = new MockMultipartFile(
                "file",
                "sales_v2.twb",
                "application/xml",
                "<?xml version='1.0'?><workbook/>".getBytes(StandardCharsets.UTF_8)
        );
        UUID wbId = UUID.randomUUID();
        JdbcWorkbookRepository.WorkbookRow v25Row = new JdbcWorkbookRepository.WorkbookRow(
                wbId,
                projectId,
                "Default",
                "Sales Report",
                "Desc",
                25,
                creatorId,
                "Creator",
                "IMPORTED",
                false,
                256L,
                "sales_v2.twb",
                Instant.now(),
                Instant.now()
        );
        JdbcWorkbookRepository.WorkbookRow v26Row = new JdbcWorkbookRepository.WorkbookRow(
                wbId,
                projectId,
                "Default",
                "Sales Report",
                "Updated",
                26,
                creatorId,
                "Creator",
                "IMPORTED",
                false,
                256L,
                "sales_v2.twb",
                Instant.now(),
                Instant.now()
        );

        when(workbookRepository.findActiveByProjectAndName(projectId, "Sales Report"))
                .thenReturn(Optional.of(v25Row));
        when(storageService.put(eq(Bucket.WORKBOOKS), anyString(), anyString(), any(), any(), anyLong()))
                .thenReturn(new StoredObject(
                        Bucket.WORKBOOKS,
                        "projects/" + projectId + "/obj-26.twb",
                        "sales_v2.twb",
                        256L,
                        "application/xml"
                ));

        UUID v1VersionId = UUID.randomUUID();
        JdbcWorkbookRepository.VersionRow oldestV1 = new JdbcWorkbookRepository.VersionRow(
                v1VersionId,
                wbId,
                1,
                "projects/" + projectId + "/obj-1.twb",
                "sales_v1.twb",
                100L,
                "Initial",
                creatorId,
                "Creator",
                Instant.now(),
                false,
                false
        );
        when(workbookRepository.findActiveVersionsExceedingLimit(wbId, 26, 25))
                .thenReturn(List.of(oldestV1));
        when(workbookRepository.countOtherVersionsUsingObjectKey(
                wbId,
                oldestV1.objectKey(),
                v1VersionId
        )).thenReturn(0);
        when(workbookRepository.findById(wbId)).thenReturn(Optional.of(v26Row));
        when(workbookRepository.toDto(v26Row)).thenReturn(new WorkbookDto(
                wbId,
                projectId,
                "Default",
                "Sales Report",
                "Updated",
                26,
                creatorId,
                "Creator",
                "IMPORTED",
                false,
                256L,
                "sales_v2.twb",
                List.of(),
                Instant.now(),
                Instant.now()
        ));

        WorkbookDto result = workbookService.uploadWorkbook(
                projectId,
                "Sales Report",
                "Updated",
                true,
                "Version 26",
                validTwb,
                creator
        );

        assertThat(result.currentVersion()).isEqualTo(26);
        verify(workbookRepository).markVersionPurged(v1VersionId);
        verify(storageService).delete(Bucket.WORKBOOKS, oldestV1.objectKey());
    }

    @Test
    void uc10RestoreDownloadAndDeleteVersionFlow() {
        UUID wbId = UUID.randomUUID();
        JdbcWorkbookRepository.WorkbookRow wbRow = new JdbcWorkbookRepository.WorkbookRow(
                wbId,
                projectId,
                "Default",
                "Finance Workbook",
                "Desc",
                2,
                creatorId,
                "Creator",
                "IMPORTED",
                false,
                512L,
                "finance.twb",
                Instant.now(),
                Instant.now()
        );
        when(workbookRepository.findById(wbId)).thenReturn(Optional.of(wbRow));

        UUID v1Id = UUID.randomUUID();
        JdbcWorkbookRepository.VersionRow v1Row = new JdbcWorkbookRepository.VersionRow(
                v1Id,
                wbId,
                1,
                "projects/obj-v1.twb",
                "finance_v1.twb",
                400L,
                "Initial",
                creatorId,
                "Creator",
                Instant.now(),
                false,
                false
        );
        JdbcWorkbookRepository.VersionRow purgedRow = new JdbcWorkbookRepository.VersionRow(
                UUID.randomUUID(),
                wbId,
                0,
                "projects/obj-v0.twb",
                "finance_v0.twb",
                300L,
                "Purged",
                creatorId,
                "Creator",
                Instant.now(),
                false,
                true
        );

        // [6a] Restoring purged version -> ERR-PRJ-09
        when(workbookRepository.findVersion(wbId, 0, 2)).thenReturn(Optional.of(purgedRow));
        assertThatThrownBy(() -> workbookService.restoreVersion(
                wbId,
                0,
                new RestoreWorkbookVersionRequest("Khôi phục bản cũ"),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_09);

        // VAL-PRJ-23: Blank restore note -> ERR-PRJ-02
        assertThatThrownBy(() -> workbookService.restoreVersion(
                wbId,
                1,
                new RestoreWorkbookVersionRequest("   "),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // BUS-PRJ-17: Restoring v1 when current is v2 creates v3 without deleting v1 or v2
        when(workbookRepository.findVersion(wbId, 1, 2)).thenReturn(Optional.of(v1Row));
        JdbcWorkbookRepository.VersionRow v3Row = new JdbcWorkbookRepository.VersionRow(
                UUID.randomUUID(),
                wbId,
                3,
                "projects/obj-v1.twb",
                "finance_v1.twb",
                400L,
                "Khôi phục từ v1",
                creatorId,
                "Creator",
                Instant.now(),
                true,
                false
        );
        when(workbookRepository.findVersion(wbId, 3, 3)).thenReturn(Optional.of(v3Row));

        WorkbookVersionDto restored = workbookService.restoreVersion(
                wbId,
                1,
                new RestoreWorkbookVersionRequest("Khôi phục từ v1"),
                creator
        );
        assertThat(restored.versionNumber()).isEqualTo(3);
        assertThat(restored.current()).isTrue();
        verify(workbookRepository).updateCurrentVersion(wbId, 3, false);

        // [3a] Download version
        when(storageService.get(Bucket.WORKBOOKS, "projects/obj-v1.twb"))
                .thenReturn(new ByteArrayInputStream("<workbook/>".getBytes(StandardCharsets.UTF_8)));
        WorkbookService.DownloadedVersion dl = workbookService.downloadVersion(wbId, 1, creator);
        assertThat(dl.fileName()).isEqualTo("finance_v1.twb");
        assertThat(dl.content()).isNotEmpty();

        // [3b] Cannot delete current version (v2)
        assertThatThrownBy(() -> workbookService.deleteVersion(wbId, 2, creator))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // [3b] Can delete non-current version (v1)
        workbookService.deleteVersion(wbId, 1, creator);
        verify(workbookRepository).deleteVersionRow(v1Id);
    }
}
