package vn.edu.davs.backend.viewer;


import vn.edu.davs.backend.service.impl.CustomViewServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.dao.ICustomViewDao;
import vn.edu.davs.backend.dao.IDashboardDao;
import vn.edu.davs.backend.dao.IProjectDao;
import vn.edu.davs.backend.dao.IWorkbookDao;
import vn.edu.davs.backend.dto.CreateCustomViewRequest;
import vn.edu.davs.backend.dto.CustomViewDto;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.SaveAsWorkbookRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.CustomViewService;
import vn.edu.davs.backend.service.PermissionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomViewAndSaveAsTests {

    @Mock
    private ICustomViewDao customViewRepository;
    @Mock
    private IDashboardDao dashboardRepository;
    @Mock
    private IWorkbookDao workbookRepository;
    @Mock
    private IProjectDao projectRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AuditService auditService;

    private CustomViewService customViewService;

    private final DavsUser author = DavsUser.of(UUID.randomUUID(), "author@davs.vn", "Author Explorer", SiteRole.EXPLORER);
    private final DavsUser otherUser = DavsUser.of(UUID.randomUUID(), "other@davs.vn", "Other User", SiteRole.VIEWER);
    private final UUID dashboardId = UUID.randomUUID();
    private final UUID customViewId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customViewService = new CustomViewServiceImpl(
                customViewRepository,
                dashboardRepository,
                workbookRepository,
                projectRepository,
                permissionService,
                auditService,
                new ObjectMapper()
        );
    }

    @Test
    void authorCanCreateCustomViewSuccessfully() {
        DashboardDto db = new DashboardDto(
                dashboardId, UUID.randomUUID(), "Dashboard", "", "{}", author.getId(), Instant.now(), Instant.now()
        );
        when(dashboardRepository.findById(dashboardId)).thenReturn(Optional.of(db));

        CustomViewDto mockCreated = new CustomViewDto(
                customViewId, dashboardId, "Doanh số Miền Bắc của tôi", Map.of("region", "Miền Bắc"), Map.of(),
                false, false, author.getId(), author.getFullName(), Instant.now(), Instant.now()
        );
        when(customViewRepository.create(eq(dashboardId), eq("Doanh số Miền Bắc của tôi"), anyString(), anyString(),
                anyBoolean(), anyBoolean(), eq(author.getId()), eq(author.getFullName())))
                .thenReturn(mockCreated);

        CreateCustomViewRequest req = new CreateCustomViewRequest(
                dashboardId, "Doanh số Miền Bắc của tôi", Map.of("region", "Miền Bắc"), Map.of(), false, false
        );

        CustomViewDto res = customViewService.create(req, author);

        assertThat(res).isNotNull();
        assertThat(res.name()).isEqualTo("Doanh số Miền Bắc của tôi");
        assertThat(res.ownerId()).isEqualTo(author.getId());
    }

    @Test
    void nonAuthorCannotSetDefaultCustomView() {
        CustomViewDto cv = new CustomViewDto(
                customViewId, dashboardId, "View của tác giả", Map.of(), Map.of(),
                false, false, author.getId(), author.getFullName(), Instant.now(), Instant.now()
        );
        when(customViewRepository.findById(customViewId)).thenReturn(Optional.of(cv));

        assertThatThrownBy(() -> customViewService.setDefault(customViewId, otherUser))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PRJ_03);
    }

    @Test
    void authorCanSetDefaultAndShareCustomView() {
        CustomViewDto cv = new CustomViewDto(
                customViewId, dashboardId, "View của tác giả", Map.of(), Map.of(),
                false, false, author.getId(), author.getFullName(), Instant.now(), Instant.now()
        );
        when(customViewRepository.findById(customViewId)).thenReturn(Optional.of(cv));

        customViewService.setDefault(customViewId, author);
        verify(customViewRepository).setDefault(customViewId, dashboardId, author.getId());

        customViewService.setShared(customViewId, true, author);
        verify(customViewRepository).setShared(customViewId, author.getId(), true);
    }

    @Test
    void saveAsWorkbookRequiresPublishPermissionInTargetProject() {
        UUID targetProjId = UUID.randomUUID();
        SaveAsWorkbookRequest req = new SaveAsWorkbookRequest(
                UUID.randomUUID(), targetProjId, "Workbook Copy", "Mô tả copy"
        );
        when(permissionService.can(eq(author), eq(Capability.PUBLISH), any(ResourceRef.class))).thenReturn(false);

        assertThatThrownBy(() -> customViewService.saveAsWorkbook(req, author))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PRJ_03);
    }

    @Test
    void saveAsWorkbookSucceedsWithPublishPermission() {
        UUID sourceWbId = UUID.randomUUID();
        UUID targetProjId = UUID.randomUUID();
        UUID copyWbId = UUID.randomUUID();
        SaveAsWorkbookRequest req = new SaveAsWorkbookRequest(
                sourceWbId, targetProjId, "Workbook Copy", "Mô tả copy"
        );
        when(permissionService.can(eq(author), eq(Capability.PUBLISH), any(ResourceRef.class))).thenReturn(true);

        IWorkbookDao.WorkbookRow source = new IWorkbookDao.WorkbookRow(
                sourceWbId, UUID.randomUUID(), "Project", "Source", "Desc", 1, author.getId(), "Author",
                "IMPORTED", false, 1024L, "source.twbx", Instant.now(), Instant.now()
        );
        when(workbookRepository.findById(sourceWbId)).thenReturn(Optional.of(source));
        when(workbookRepository.findActiveByProjectAndName(targetProjId, "Workbook Copy")).thenReturn(Optional.empty());

        when(workbookRepository.insertWorkbook(eq(targetProjId), eq("Workbook Copy"), anyString(),
                eq(author.getId()), anyString(), eq(false)))
                .thenReturn(copyWbId);

        IWorkbookDao.WorkbookRow copyRow = new IWorkbookDao.WorkbookRow(
                copyWbId, targetProjId, "Project", "Workbook Copy", "Mô tả copy", 1, author.getId(), "Author",
                "IMPORTED", false, 1024L, "copy.twbx", Instant.now(), Instant.now()
        );
        when(workbookRepository.findById(copyWbId)).thenReturn(Optional.of(copyRow));

        WorkbookDto copyDto = new WorkbookDto(
                copyWbId, targetProjId, "Project", "Workbook Copy", "Mô tả copy", 1, author.getId(),
                "Author", "IMPORTED", false, 1024L, "copy.twbx", List.of(), Instant.now(), Instant.now()
        );
        when(workbookRepository.toDto(copyRow)).thenReturn(copyDto);

        WorkbookDto res = customViewService.saveAsWorkbook(req, author);

        assertThat(res).isNotNull();
        assertThat(res.name()).isEqualTo("Workbook Copy");
    }
}
