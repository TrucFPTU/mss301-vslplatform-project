package com.vsl.social.service;

import com.vsl.social.dto.LikeResponse;

public interface LikeService {

    LikeResponse like(Long blogId, Long userId);

    LikeResponse unlike(Long blogId, Long userId);
}
