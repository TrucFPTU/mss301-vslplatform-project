package com.vsl.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request/response của các API AI xử lý văn bản (Gemini). Gom một chỗ vì đều là record nhỏ. */
public final class ContentAiDtos {

    private ContentAiDtos() {
    }

    /** Kiểm duyệt một bài viết (tiêu đề + nội dung) hoặc một bình luận (chỉ content). */
    public record ModerationRequest(
            @Size(max = 500) String title,
            @NotBlank @Size(max = 50_000) String content) {
    }

    /**
     * @param allowed  có cho đăng không
     * @param reason   lý do (tiếng Việt) khi bị chặn
     * @param checked  false = AI đang tắt / chưa cấu hình key nên không kiểm (cho đăng)
     */
    public record ModerationResult(boolean allowed, String reason, boolean checked) {
    }

    public record ExistingBlog(@NotNull Long id, String title, String content) {
    }

    /** Bài mới có phải bản sao / viết lại của một bài đã có không. */
    public record DuplicateCheckRequest(
            @Size(max = 500) String title,
            @NotBlank @Size(max = 50_000) String content,
            @NotNull @Size(max = 50) @Valid List<ExistingBlog> existing) {
    }

    public record DuplicateCheckResult(boolean duplicate, Long matchedBlogId, String reason, boolean checked) {
    }

    public record VocabEntry(@NotBlank String word, String categoryName) {
    }

    /** Tìm trong danh sách từ đã có những từ đồng nghĩa với từ ứng viên. */
    public record SynonymRequest(
            @NotBlank @Size(max = 255) String word,
            @NotNull @Size(max = 2000) @Valid List<VocabEntry> existing) {
    }

    /** @param checked false = AI đang tắt, synonyms luôn rỗng */
    public record SynonymResult(boolean checked, List<VocabEntry> synonyms) {
    }
}
