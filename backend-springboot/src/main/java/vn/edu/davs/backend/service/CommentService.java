package vn.edu.davs.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.dao.JdbcCommentRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.CommentDto;
import vn.edu.davs.backend.dto.CreateCommentRequest;
import vn.edu.davs.backend.dto.CreateReplyRequest;
import vn.edu.davs.backend.dto.UpdateCommentRequest;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final JdbcCommentRepository commentRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CommentDto> listComments(String resourceType, UUID resourceId, DavsUser actor) {
        ResourceType rType = parseResourceType(resourceType);
        ResourceRef ref = new ResourceRef(rType, resourceId);

        if (!permissionService.can(actor, Capability.VIEW, ref)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Bạn không có quyền xem bình luận trên tài nguyên này");
        }

        boolean isAdminOrOwner = actor.getSiteRole() == SiteRole.SITE_ADMIN;
        return commentRepository.findTreeByResource(rType.name(), resourceId, actor.getId(), isAdminOrOwner);
    }

    @Transactional
    public CommentDto createComment(String resourceType, UUID resourceId, CreateCommentRequest request, DavsUser actor) {
        ResourceType rType = parseResourceType(resourceType);
        ResourceRef ref = new ResourceRef(rType, resourceId);

        // DIS-SHR-02: Require COMMENT permission
        if (!permissionService.can(actor, Capability.COMMENT, ref)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Bạn không có quyền đăng bình luận trên tài nguyên này");
        }

        validateCommentText(request.text());

        if (request.mentions() != null && request.mentions().size() > 10) {
            throw new DavsException(ErrorCode.ERR_CMT_01, "Tối đa 10 người được nhắc tên (@)");
        }

        String mentionsJson = writeJson(request.mentions());
        CommentDto created = commentRepository.create(
                rType.name(),
                resourceId,
                request.parentId(),
                actor.getId(),
                request.text().trim(),
                mentionsJson,
                request.snapshotUrl(),
                actor.getFullName(),
                actor.getEmail()
        );

        auditService.log(AuditEvent.of("COMMENT_CREATE", actor.getId(), actor.getEmail(), rType.name(), resourceId.toString()));

        return created;
    }

    @Transactional
    public void updateComment(UUID commentId, UpdateCommentRequest request, DavsUser actor) {
        validateCommentText(request.text());

        CommentDto comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy bình luận"));

        if (!comment.authorId().equals(actor.getId())) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Chỉ tác giả mới có thể sửa bình luận");
        }

        // BUS-SHR-11: Allowed within 24 hours
        if (Duration.between(comment.createdAt(), Instant.now()).toHours() >= 24) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Chỉ được phép sửa bình luận trong vòng 24 giờ kể từ khi tạo");
        }

        commentRepository.update(commentId, request.text().trim());

        auditService.log(AuditEvent.of("COMMENT_UPDATE", actor.getId(), actor.getEmail(),
                comment.resourceType(), comment.resourceId().toString()));
    }

    @Transactional
    public void deleteComment(UUID commentId, DavsUser actor) {
        CommentDto comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy bình luận"));

        // BUS-SHR-11: Author anytime; site admin anytime
        boolean isAuthor = comment.authorId().equals(actor.getId());
        boolean isAdmin = actor.getSiteRole() == SiteRole.SITE_ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(commentId);

        auditService.log(AuditEvent.of("COMMENT_DELETE", actor.getId(), actor.getEmail(),
                comment.resourceType(), comment.resourceId().toString()));
    }

    @Transactional
    public CommentDto createReply(UUID parentCommentId, CreateReplyRequest request, DavsUser actor) {
        CommentDto parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy bình luận gốc"));

        CreateCommentRequest commentReq = new CreateCommentRequest(
                request.text(),
                request.mentions(),
                false,
                null,
                parentCommentId
        );

        return createComment(parent.resourceType(), parent.resourceId(), commentReq, actor);
    }

    private void validateCommentText(String text) {
        if (text == null || text.trim().isEmpty() || text.trim().length() > 1000) {
            throw new DavsException(ErrorCode.ERR_CMT_01, "Nội dung bình luận phải từ 1 đến 1.000 ký tự");
        }
    }

    private ResourceType parseResourceType(String type) {
        if (type == null) {
            return ResourceType.DASHBOARD;
        }
        try {
            return ResourceType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return ResourceType.DASHBOARD;
        }
    }

    private String writeJson(Object obj) {
        if (obj == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
