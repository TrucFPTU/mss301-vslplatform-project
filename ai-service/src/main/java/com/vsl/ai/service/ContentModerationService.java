package com.vsl.ai.service;

import com.vsl.ai.dto.ContentAiDtos.DuplicateCheckRequest;
import com.vsl.ai.dto.ContentAiDtos.DuplicateCheckResult;
import com.vsl.ai.dto.ContentAiDtos.ModerationRequest;
import com.vsl.ai.dto.ContentAiDtos.ModerationResult;
import com.vsl.ai.dto.ContentAiDtos.SynonymRequest;
import com.vsl.ai.dto.ContentAiDtos.SynonymResult;

/**
 * Xử lý văn bản bằng Gemini (bê từ GeminiModerationService của monolith).
 * Chưa cấu hình GEMINI_API_KEY / gemini.enabled=false -> bỏ qua (checked=false), để môi trường dev vẫn chạy.
 */
public interface ContentModerationService {

    /** Kiểm duyệt nội dung. Lỗi gọi API -> FAIL-CLOSED: ném MODERATION_ERROR (không để lọt nội dung chưa kiểm). */
    ModerationResult moderate(ModerationRequest request);

    /** Phát hiện bài đăng lại / viết lại (paraphrase) của một bài đã có. Lỗi -> MODERATION_ERROR. */
    DuplicateCheckResult checkDuplicate(DuplicateCheckRequest request);

    /** Tìm từ đồng nghĩa trong danh sách đã có. Lỗi gọi API -> AI_EXTERNAL_ERROR (để bên gọi báo "AI lỗi"). */
    SynonymResult findSynonyms(SynonymRequest request);

    boolean isEnabled();
}
