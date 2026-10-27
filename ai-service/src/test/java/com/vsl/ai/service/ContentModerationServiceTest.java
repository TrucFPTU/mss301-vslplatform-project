package com.vsl.ai.service;

import com.vsl.ai.dto.ContentAiDtos.DuplicateCheckRequest;
import com.vsl.ai.dto.ContentAiDtos.DuplicateCheckResult;
import com.vsl.ai.dto.ContentAiDtos.ExistingBlog;
import com.vsl.ai.dto.ContentAiDtos.ModerationRequest;
import com.vsl.ai.dto.ContentAiDtos.ModerationResult;
import com.vsl.ai.dto.ContentAiDtos.SynonymRequest;
import com.vsl.ai.dto.ContentAiDtos.SynonymResult;
import com.vsl.ai.dto.ContentAiDtos.VocabEntry;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentModerationServiceTest {

    /** Thay callGemini bằng phản hồi cố định -> test phần phân tích mà không gọi mạng. */
    private static ContentModerationServiceImpl withResponse(String geminiText) {
        ContentModerationServiceImpl service = new ContentModerationServiceImpl() {
            @Override
            String callGemini(String prompt, ErrorCode errorCode) {
                if (geminiText == null) {
                    return "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
                }
                return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quote(geminiText) + "}]}}]}";
            }
        };
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "model", "gemini-test");
        return service;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    @Test
    @DisplayName("Chưa cấu hình key -> không kiểm, cho đăng (checked=false)")
    void disabled_AllowsWithoutCheck() {
        ContentModerationServiceImpl service = new ContentModerationServiceImpl();
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "enabled", true);

        ModerationResult result = service.moderate(new ModerationRequest("t", "c"));

        assertThat(result.allowed()).isTrue();
        assertThat(result.checked()).isFalse();
    }

    @Test
    @DisplayName("Gemini chặn -> allowed=false kèm lý do")
    void moderate_Blocked() {
        ModerationResult result = withResponse("{\"allowed\": false, \"reason\": \"Quảng cáo\"}")
                .moderate(new ModerationRequest("Mua ngay", "giảm giá"));

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("Quảng cáo");
        assertThat(result.checked()).isTrue();
    }

    @Test
    @DisplayName("Gemini tự chặn prompt (không có candidate) -> coi là vi phạm")
    void moderate_NoCandidates_Blocked() {
        ModerationResult result = withResponse(null).moderate(new ModerationRequest("t", "c"));

        assertThat(result.allowed()).isFalse();
    }

    @Test
    @DisplayName("Phản hồi thiếu trường allowed -> mặc định KHÔNG cho phép")
    void moderate_MissingField_Blocked() {
        assertThat(withResponse("{\"reason\": \"\"}").moderate(new ModerationRequest("t", "c")).allowed()).isFalse();
    }

    @Test
    @DisplayName("Phản hồi không phải JSON -> MODERATION_ERROR (fail-closed)")
    void moderate_Garbage_FailClosed() {
        assertThatThrownBy(() -> withResponse("không phải json").moderate(new ModerationRequest("t", "c")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODERATION_ERROR);
    }

    @Test
    @DisplayName("checkDuplicate: id do model bịa (không có trong danh sách) bị bỏ")
    void duplicate_InventedIdDropped() {
        DuplicateCheckResult result = withResponse("{\"duplicate\": true, \"matchedBlogId\": 999, \"reason\": \"x\"}")
                .checkDuplicate(new DuplicateCheckRequest("t", "c", List.of(new ExistingBlog(1L, "a", "b"))));

        assertThat(result.duplicate()).isTrue();
        assertThat(result.matchedBlogId()).isNull();
    }

    @Test
    @DisplayName("checkDuplicate: id hợp lệ được giữ")
    void duplicate_ValidId() {
        DuplicateCheckResult result = withResponse("{\"duplicate\": true, \"matchedBlogId\": 1, \"reason\": \"x\"}")
                .checkDuplicate(new DuplicateCheckRequest("t", "c", List.of(new ExistingBlog(1L, "a", "b"))));

        assertThat(result.matchedBlogId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findSynonyms: chỉ giữ từ có trong danh sách và khác từ ứng viên")
    void synonyms_FilterInventedAndSelf() {
        SynonymResult result = withResponse("{\"synonyms\":[{\"word\":\"lo sợ\",\"categoryName\":\"Cảm xúc\"},"
                + "{\"word\":\"từ bịa\",\"categoryName\":\"?\"},{\"word\":\"sợ hãi\",\"categoryName\":\"Cảm xúc\"}]}")
                .findSynonyms(new SynonymRequest("sợ hãi", List.of(
                        new VocabEntry("lo sợ", "Cảm xúc"),
                        new VocabEntry("sợ hãi", "Cảm xúc"),
                        new VocabEntry("vui", "Cảm xúc"))));

        assertThat(result.checked()).isTrue();
        assertThat(result.synonyms()).containsExactly(new VocabEntry("lo sợ", "Cảm xúc"));
    }
}
