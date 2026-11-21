package vn.edu.davs.backend.viewer;


import vn.edu.davs.backend.service.impl.CommentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.dao.ICommentDao;
import vn.edu.davs.backend.dto.CommentDto;
import vn.edu.davs.backend.dto.CreateCommentRequest;
import vn.edu.davs.backend.dto.UpdateCommentRequest;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.CommentService;
import vn.edu.davs.backend.service.PermissionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAndReplyTests {

    @Mock
    private ICommentDao commentRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AuditService auditService;

    private CommentService commentService;

    private final DavsUser author = DavsUser.of(UUID.randomUUID(), "author@davs.vn", "Author", SiteRole.VIEWER);
    private final DavsUser otherUser = DavsUser.of(UUID.randomUUID(), "other@davs.vn", "Other", SiteRole.VIEWER);
    private final DavsUser admin = DavsUser.of(UUID.randomUUID(), "admin@davs.vn", "Admin", SiteRole.SITE_ADMIN);
    private final UUID resourceId = UUID.randomUUID();
    private final UUID commentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(
                commentRepository,
                permissionService,
                auditService,
                new ObjectMapper()
        );
    }

    @Test
    void userCanPostCommentSuccessfully() {
        when(permissionService.can(eq(author), eq(Capability.COMMENT), any(ResourceRef.class))).thenReturn(true);

        CommentDto mockCreated = new CommentDto(
                commentId, "DASHBOARD", resourceId, null, author.getId(), author.getFullName(), author.getEmail(),
                "Số liệu tháng 7 rất ấn tượng", Collections.emptyList(), null, true, true,
                Instant.now(), Instant.now(), Collections.emptyList()
        );
        when(commentRepository.create(eq("DASHBOARD"), eq(resourceId), any(), eq(author.getId()),
                eq("Số liệu tháng 7 rất ấn tượng"), any(), any(), eq(author.getFullName()), eq(author.getEmail())))
                .thenReturn(mockCreated);

        CreateCommentRequest req = new CreateCommentRequest("Số liệu tháng 7 rất ấn tượng", Collections.emptyList(), false, null, null);

        CommentDto res = commentService.createComment("DASHBOARD", resourceId, req, author);

        assertThat(res).isNotNull();
        assertThat(res.text()).isEqualTo("Số liệu tháng 7 rất ấn tượng");
        assertThat(res.authorId()).isEqualTo(author.getId());
    }

    @Test
    void emptyOrOver1000CharsCommentThrowsERR_CMT_01() {
        when(permissionService.can(eq(author), eq(Capability.COMMENT), any(ResourceRef.class))).thenReturn(true);

        CreateCommentRequest emptyReq = new CreateCommentRequest("", Collections.emptyList(), false, null, null);
        assertThatThrownBy(() -> commentService.createComment("DASHBOARD", resourceId, emptyReq, author))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_CMT_01);

        String longText = "A".repeat(1001);
        CreateCommentRequest longReq = new CreateCommentRequest(longText, Collections.emptyList(), false, null, null);
        assertThatThrownBy(() -> commentService.createComment("DASHBOARD", resourceId, longReq, author))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_CMT_01);
    }

    @Test
    void authorCanEditCommentWithin24Hours() {
        CommentDto existing = new CommentDto(
                commentId, "DASHBOARD", resourceId, null, author.getId(), author.getFullName(), author.getEmail(),
                "Ban đầu", Collections.emptyList(), null, true, true,
                Instant.now().minus(2, ChronoUnit.HOURS), Instant.now(), Collections.emptyList()
        );
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        commentService.updateComment(commentId, new UpdateCommentRequest("Đã sửa đổi"), author);

        verify(commentRepository).update(commentId, "Đã sửa đổi");
    }

    @Test
    void nonAuthorCannotEditComment() {
        CommentDto existing = new CommentDto(
                commentId, "DASHBOARD", resourceId, null, author.getId(), author.getFullName(), author.getEmail(),
                "Ban đầu", Collections.emptyList(), null, true, true, Instant.now(), Instant.now(), Collections.emptyList()
        );
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.updateComment(commentId, new UpdateCommentRequest("Sửa trộm"), otherUser))
                .isInstanceOf(DavsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PRJ_03);
    }

    @Test
    void authorOrAdminCanDeleteComment() {
        CommentDto existing = new CommentDto(
                commentId, "DASHBOARD", resourceId, null, author.getId(), author.getFullName(), author.getEmail(),
                "Xóa tôi đi", Collections.emptyList(), null, true, true, Instant.now(), Instant.now(), Collections.emptyList()
        );
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        // Author deletes
        commentService.deleteComment(commentId, author);

        // Admin deletes
        commentService.deleteComment(commentId, admin);

        verify(commentRepository, org.mockito.Mockito.times(2)).delete(commentId);
    }
}
