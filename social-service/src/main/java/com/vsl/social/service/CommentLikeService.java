package com.vsl.social.service;

import com.vsl.social.dto.CommentLikeResponse;

public interface CommentLikeService {

    CommentLikeResponse likeComment(Long commentId, Long userId);

    CommentLikeResponse unlikeComment(Long commentId, Long userId);

    CommentLikeResponse likeReply(Long replyId, Long userId);

    CommentLikeResponse unlikeReply(Long replyId, Long userId);
}
