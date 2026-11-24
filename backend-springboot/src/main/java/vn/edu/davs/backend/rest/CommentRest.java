package vn.edu.davs.backend.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.davs.backend.dto.CommentDto;
import vn.edu.davs.backend.dto.CreateCommentRequest;
import vn.edu.davs.backend.dto.CreateReplyRequest;
import vn.edu.davs.backend.dto.UpdateCommentRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "UC24 - Comments & Threaded Discussions", description = "Comment threads, replies, mentions and snapshot attachments")
public interface CommentRest {


    @Operation(summary = "List comment threads on resource (dashboard or workbook)")
    @GetMapping("/resources/{type}/{id}/comments")
    ResponseEntity<List<CommentDto>> listComments(
            @PathVariable String type,
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Post a comment on resource")
    @PostMapping("/resources/{type}/{id}/comments")
    ResponseEntity<CommentDto> createComment(
            @PathVariable String type,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Update comment text (author only, within 24 hours)")
    @PatchMapping("/comments/{id}")
    ResponseEntity<Map<String, String>> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Delete comment (author or admin/owner)")
    @DeleteMapping("/comments/{id}")
    ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );


    @Operation(summary = "Reply to a comment thread")
    @PostMapping("/comments/{id}/replies")
    ResponseEntity<CommentDto> createReply(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal DavsUser actor
    );
}
