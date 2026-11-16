package vn.edu.davs.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.CommentDto;
import vn.edu.davs.backend.dto.CreateCommentRequest;
import vn.edu.davs.backend.dto.CreateReplyRequest;
import vn.edu.davs.backend.dto.UpdateCommentRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.CommentService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UC24 - Comments & Threaded Discussions", description = "Comment threads, replies, mentions and snapshot attachments")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "List comment threads on resource (dashboard or workbook)")
    @GetMapping({"/resources/{type}/{id}/comments", "/dashboards/{id}/comments"})
    public ResponseEntity<List<CommentDto>> listComments(
            @PathVariable(required = false) String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        String resType = type != null ? type : "DASHBOARD";
        return ResponseEntity.ok(commentService.listComments(resType, id, actor));
    }

    @Operation(summary = "Post a comment on resource")
    @PostMapping({"/resources/{type}/{id}/comments", "/dashboards/{id}/comments"})
    public ResponseEntity<CommentDto> createComment(
            @PathVariable(required = false) String type,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        String resType = type != null ? type : "DASHBOARD";
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(resType, id, request, actor));
    }

    @Operation(summary = "Update comment text (author only, within 24 hours)")
    @RequestMapping(value = "/comments/{id}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Map<String, String>> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        commentService.updateComment(id, request, actor);
        return ResponseEntity.ok(Map.of("message", "Cập nhật bình luận thành công"));
    }

    @Operation(summary = "Delete comment (author or admin/owner)")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        commentService.deleteComment(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reply to a comment thread")
    @PostMapping("/comments/{id}/replies")
    public ResponseEntity<CommentDto> createReply(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createReply(id, request, actor));
    }
}
