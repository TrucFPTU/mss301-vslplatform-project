package com.vsl.learning.dto.response;

import com.vsl.learning.entity.SuggestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VocabularySuggestionResponse {
    private Long id;
    private String word;
    private String description;
    private SuggestionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // Danh mục đề xuất
    private Long categoryId;
    private String categoryName;

    // Người gửi đề xuất (chỉ lưu id - tên người dùng do identity-service quản lý).
    private Long requesterId;
}
