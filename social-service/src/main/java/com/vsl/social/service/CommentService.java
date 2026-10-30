package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.ReplyResponse;

public interface CommentService {

    PageResponse<CommentResponse> listComments(Long blogId, Long viewerId, int page, int size);

    CommentResponse addComment(Long blogId, Long userId, CommentRequest req);

    CommentResponse updateComment(Long commentId, Long userId, CommentRequest req);

    void deleteComment(Long commentId, Long userId);

    PageResponse<ReplyResponse> listReplies(Long commentId, Long viewerId, int page, int size);

    ReplyResponse addReply(Long commentId, Long userId, CommentRequest req);

    ReplyResponse updateReply(Long replyId, Long userId, CommentRequest req);

    void deleteReply(Long replyId, Long userId);
}
