package vn.edu.davs.backend.rest.impl;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.CommentRest;

@RequiredArgsConstructor
@RestController
public class CommentRestImpl implements CommentRest {


    private final CommentService commentService;

    @Override
    public ResponseEntity<List<CommentDto>> listComments(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(commentService.listComments(type, id, actor));
    }

    @Override
    public ResponseEntity<CommentDto> createComment(
            @PathVariable String type,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(type, id, request, actor));
    }

    @Override
    public ResponseEntity<Map<String, String>> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        commentService.updateComment(id, request, actor);
        return ResponseEntity.ok(Map.of("message", "Cập nhật bình luận thành công"));
    }

    @Override
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        commentService.deleteComment(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CommentDto> createReply(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createReply(id, request, actor));
    }
}
