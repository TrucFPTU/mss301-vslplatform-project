package com.vsl.social.dto;

import com.vsl.common.response.PageResponse;

/** Kết quả tìm kiếm: bài viết công khai + người dùng khớp từ khóa (như monolith). */
public record BlogSearchResponse(
        PageResponse<BlogResponse> blogs,
        PageResponse<SocialUserResponse> users
) {
}
