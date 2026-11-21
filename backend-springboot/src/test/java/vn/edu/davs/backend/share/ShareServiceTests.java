package vn.edu.davs.backend.share;


import vn.edu.davs.backend.service.impl.ShareServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import vn.edu.davs.backend.dto.ShareRequest;
import vn.edu.davs.backend.dto.ShareResponse;
import vn.edu.davs.backend.enu.EmailTemplate;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.NotificationService;
import vn.edu.davs.backend.service.PermissionService;
import vn.edu.davs.backend.service.ShareService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class ShareServiceTests {

    private JdbcTemplate jdbcTemplate;
    private PermissionService permissionService;
    private NotificationService notificationService;
    private AuditService auditService;
    private ShareService shareService;

    private DavsUser creatorUser;
    private DavsUser viewerUser;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        permissionService = mock(PermissionService.class);
        notificationService = mock(NotificationService.class);
        auditService = mock(AuditService.class);
        shareService = new ShareServiceImpl(jdbcTemplate, permissionService, notificationService, auditService);

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
    void testShareSuccess() {
        UUID resId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), eq("colleague@congty.vn"), eq("colleague@congty.vn")))
                .thenReturn(Map.of(
                        "id", recipientUserId,
                        "email", "colleague@congty.vn",
                        "full_name", "Colleague User"
                ));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(resId)))
                .thenReturn("Báo cáo tháng");

        ShareRequest req = new ShareRequest(
                "WORKBOOK",
                resId,
                List.of("colleague@congty.vn"),
                "VIEW",
                "Mời bạn xem",
                true
        );

        ShareResponse res = shareService.shareContent(req, creatorUser);

        assertNotNull(res);
        assertTrue(res.success());
        assertEquals(1, res.sharedCount());
        assertNotNull(res.shareLink());
        assertNotNull(res.embedCode());
        verify(notificationService).sendAsync(eq(EmailTemplate.SHARE), eq("colleague@congty.vn"), any());
        verify(auditService).log(any());
    }

    @Test
    void testShareRecipientNotFoundThrowsError() {
        UUID resId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), any(), any()))
                .thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        ShareRequest req = new ShareRequest(
                "WORKBOOK",
                resId,
                List.of("unknown_user"),
                "VIEW",
                "Mời bạn xem",
                false
        );

        DavsException ex = assertThrows(DavsException.class, () ->
                shareService.shareContent(req, creatorUser)
        );
        assertEquals(ErrorCode.ERR_SHR_01, ex.getErrorCode());
    }

    @Test
    void testShareExternalRecipientThrowsForbidden() {
        UUID resId = UUID.randomUUID();

        when(jdbcTemplate.queryForMap(anyString(), any(), any()))
                .thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        ShareRequest req = new ShareRequest(
                "WORKBOOK",
                resId,
                List.of("outsider@gmail.com"),
                "VIEW",
                "Mời bạn xem",
                false
        );

        // ERR-SHR-02: external policy forbids sharing
        DavsException ex = assertThrows(DavsException.class, () ->
                shareService.shareContent(req, creatorUser)
        );
        assertEquals(ErrorCode.ERR_SHR_02, ex.getErrorCode());
    }

    @Test
    void testViewerCannotGrantEditPermission() {
        UUID resId = UUID.randomUUID();

        ShareRequest req = new ShareRequest(
                "WORKBOOK",
                resId,
                List.of("colleague@congty.vn"),
                "EDIT", // Viewer trying to grant EDIT (BUS-SHR-06)
                "Mời chỉnh sửa",
                false
        );

        DavsException ex = assertThrows(DavsException.class, () ->
                shareService.shareContent(req, viewerUser)
        );
        assertEquals(ErrorCode.ERR_PRJ_03, ex.getErrorCode());
    }

    @Test
    void testMaxRecipientsExceededThrowsException() {
        UUID resId = UUID.randomUUID();
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            largeList.add("user" + i + "@congty.vn");
        }

        ShareRequest req = new ShareRequest(
                "WORKBOOK",
                resId,
                largeList,
                "VIEW",
                null,
                false
        );

        // BUS-SHR-07: max 100 recipients
        assertThrows(IllegalArgumentException.class, () ->
                shareService.shareContent(req, creatorUser)
        );
    }
}
