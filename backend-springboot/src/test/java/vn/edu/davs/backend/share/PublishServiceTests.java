package vn.edu.davs.backend.share;


import vn.edu.davs.backend.service.impl.PublishServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.PublishWorkbookRequest;
import vn.edu.davs.backend.dto.PublishWorkbookResponse;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.common.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.PublishService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PublishServiceTests {

    private JdbcTemplate jdbcTemplate;
    private PermissionService permissionService;
    private AuditService auditService;
    private ObjectMapper objectMapper;
    private PublishService publishService;

    private DavsUser creatorUser;
    private DavsUser viewerUser;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        permissionService = mock(PermissionService.class);
        auditService = mock(AuditService.class);
        objectMapper = new ObjectMapper();
        publishService = new PublishServiceImpl(jdbcTemplate, permissionService, auditService, objectMapper);

        creatorUser = DavsUser.of(
                UUID.randomUUID(),
                "creator@congty.vn",
                "Creator User",
                SiteRole.CREATOR
        );

        viewerUser = DavsUser.of(
                UUID.randomUUID(),
                "viewer@congty.vn",
                "Viewer User",
                SiteRole.VIEWER
        );
    }

    @Test
    void testPublishSuccess() {
        UUID workbookId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), eq(workbookId))).thenReturn(Map.of(
                "id", workbookId,
                "project_id", projectId,
                "name", "Báo cáo Doanh thu",
                "description", "Mô tả",
                "current_version", 1,
                "needs_reconnect", false
        ));

        when(permissionService.can(eq(creatorUser), eq(Capability.PUBLISH), any())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq(projectId), anyString())).thenReturn(null);

        PublishWorkbookRequest request = new PublishWorkbookRequest(
                "INTERNAL_SERVER",
                projectId,
                "Báo cáo Doanh thu",
                "Bản xuất bản",
                List.of("Sheet 1", "Sheet 2"),
                "EMBEDDED",
                true,
                true,
                false
        );

        PublishWorkbookResponse response = publishService.publishWorkbook(workbookId, request, creatorUser);

        assertNotNull(response);
        assertEquals("INTERNAL_SERVER", response.destination());
        assertEquals("Báo cáo Doanh thu", response.publishedName());
        assertNotNull(response.url());
        verify(auditService).log(any());
    }

    @Test
    void testPublishInsufficientPermissionsFails() {
        UUID workbookId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), eq(workbookId))).thenReturn(Map.of(
                "id", workbookId,
                "project_id", projectId,
                "name", "Báo cáo Doanh thu",
                "current_version", 1,
                "needs_reconnect", false
        ));

        PublishWorkbookRequest request = new PublishWorkbookRequest(
                "INTERNAL_SERVER",
                projectId,
                "Báo cáo",
                null,
                List.of(),
                "EMBEDDED",
                false,
                true,
                false
        );

        // Viewer does not have CREATOR role (BUS-SHR-01)
        DavsException ex = assertThrows(DavsException.class, () ->
                publishService.publishWorkbook(workbookId, request, viewerUser)
        );
        assertEquals(ErrorCode.ERR_PUB_01, ex.getErrorCode());
    }

    @Test
    void testPublishPublicWithConfidentialDataFails() {
        UUID workbookId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), eq(workbookId))).thenReturn(Map.of(
                "id", workbookId,
                "project_id", projectId,
                "name", "Báo cáo Tài chính Mật",
                "current_version", 1,
                "needs_reconnect", false
        ));
        when(permissionService.can(any(), any(), any())).thenReturn(true);

        // Mock confidential label count > 0 (BUS-SHR-03)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(workbookId))).thenReturn(1);

        PublishWorkbookRequest request = new PublishWorkbookRequest(
                "PUBLIC",
                projectId,
                "Báo cáo Tài chính Mật",
                null,
                List.of(),
                "EMBEDDED",
                false,
                false,
                false
        );

        DavsException ex = assertThrows(DavsException.class, () ->
                publishService.publishWorkbook(workbookId, request, creatorUser)
        );
        assertEquals(ErrorCode.ERR_PUB_01, ex.getErrorCode());
    }

    @Test
    void testPublishDuplicateNameWithoutOverwriteThrowsConflict() {
        UUID workbookId = UUID.randomUUID();
        UUID targetProjectId = UUID.randomUUID();
        UUID existingOtherWorkbookId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), eq(workbookId))).thenReturn(Map.of(
                "id", workbookId,
                "project_id", targetProjectId,
                "name", "Báo cáo",
                "current_version", 1,
                "needs_reconnect", false
        ));
        when(permissionService.can(any(), any(), any())).thenReturn(true);
        // Exists workbook with same name (ERR-PUB-02)
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq(targetProjectId), anyString()))
                .thenReturn(existingOtherWorkbookId);

        PublishWorkbookRequest request = new PublishWorkbookRequest(
                "INTERNAL_SERVER",
                targetProjectId,
                "Báo cáo",
                null,
                List.of(),
                "EMBEDDED",
                false,
                false,
                false // overwrite = false
        );

        DavsException ex = assertThrows(DavsException.class, () ->
                publishService.publishWorkbook(workbookId, request, creatorUser)
        );
        assertEquals(ErrorCode.ERR_PUB_02, ex.getErrorCode());
    }

    @Test
    void testPublishBrokenDatasourceThrowsError() {
        UUID workbookId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), eq(workbookId))).thenReturn(Map.of(
                "id", workbookId,
                "project_id", projectId,
                "name", "Báo cáo",
                "current_version", 1,
                "needs_reconnect", true // broken connection
        ));
        when(permissionService.can(any(), any(), any())).thenReturn(true);

        PublishWorkbookRequest request = new PublishWorkbookRequest(
                "INTERNAL_SERVER",
                projectId,
                "Báo cáo",
                null,
                List.of(),
                "EMBEDDED",
                false,
                false,
                true
        );

        DavsException ex = assertThrows(DavsException.class, () ->
                publishService.publishWorkbook(workbookId, request, creatorUser)
        );
        assertEquals(ErrorCode.ERR_PUB_03, ex.getErrorCode());
    }
}
