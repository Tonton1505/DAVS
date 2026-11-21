package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CommentDto;
import vn.edu.davs.backend.dto.CreateCommentRequest;
import vn.edu.davs.backend.dto.CreateReplyRequest;
import vn.edu.davs.backend.dto.UpdateCommentRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public interface CommentService {


    @Transactional(readOnly = true)
    List<CommentDto> listComments(String resourceType, UUID resourceId, DavsUser actor);


    @Transactional
    CommentDto createComment(String resourceType, UUID resourceId, CreateCommentRequest request, DavsUser actor);


    @Transactional
    void updateComment(UUID commentId, UpdateCommentRequest request, DavsUser actor);


    @Transactional
    void deleteComment(UUID commentId, DavsUser actor);


    @Transactional
    CommentDto createReply(UUID parentCommentId, CreateReplyRequest request, DavsUser actor);
}
