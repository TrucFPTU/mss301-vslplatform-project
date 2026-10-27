package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.text.VietnameseText;
import com.vsl.social.client.AiModerationClient;
import com.vsl.social.dto.remote.RemoteDtos.DuplicateCheckResult;
import com.vsl.social.dto.remote.RemoteDtos.ExistingBlog;
import com.vsl.social.dto.remote.RemoteDtos.ModerationResult;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.repository.BlogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlogPublishGuardTest {

    @Mock
    BlogRepository blogRepository;
    @Mock
    AiModerationClient ai;
    @InjectMocks
    BlogPublishGuard guard;

    @BeforeEach
    void allowByDefault() {
        when(ai.moderate(anyString(), anyString())).thenReturn(new ModerationResult(true, "", true));
        when(ai.checkDuplicate(anyString(), anyString(), anyList())).thenReturn(new DuplicateCheckResult(false, null, "", true));
        when(blogRepository.findDuplicateCandidates(any(), any())).thenReturn(List.of());
    }

    private static Blog blog(long id, String title, String content) {
        Blog b = Blog.builder().authorId(1L).title(title).content(content).status(BlogStatus.PUBLISHED).build();
        b.setId(id);
        return b;
    }

    @Test
    void reportedContent_blockedBeforeAnyAiCall() {
        String fp = VietnameseText.fingerprint("Nội dung");
        when(blogRepository.existsReportedWithContent("tieu de", fp, null)).thenReturn(true);

        assertAppError(() -> guard.checkBeforePublish("Tiêu Đề", "Nội dung", null), ErrorCode.BLOG_CONTENT_ALREADY_REPORTED);
        verify(ai, never()).moderate(anyString(), anyString());
    }

    @Test
    void exactDuplicate_ignoresAccentsCaseAndSpaces() {
        when(blogRepository.existsPublishedWithContent(eq("hoc ngon ngu ky hieu"),
                eq(VietnameseText.fingerprint("bai hoc dau tien")), isNull())).thenReturn(true);

        assertAppError(() -> guard.checkBeforePublish("Học  Ngôn ngữ KÝ HIỆU", "Bài học   đầu tiên", null),
                ErrorCode.BLOG_DUPLICATE_CONTENT);
    }

    @Test
    void duplicateTitle_blocked() {
        when(blogRepository.existsPublishedWithTitle("xin chao", 5L)).thenReturn(true);

        assertAppError(() -> guard.checkBeforePublish("Xin chào", "khác", 5L), ErrorCode.BLOG_DUPLICATE_TITLE);
    }

    @Test
    void semanticDuplicate_blockedWithAiReason() {
        when(blogRepository.findDuplicateCandidates(isNull(), any())).thenReturn(List.of(
                blog(9L, "Cách chào hỏi bằng ký hiệu", "hướng dẫn chào hỏi bằng ngôn ngữ ký hiệu cho người mới")));
        when(ai.checkDuplicate(anyString(), anyString(), anyList()))
                .thenReturn(new DuplicateCheckResult(true, 9L, "Viết lại bài 9", true));

        assertThatThrownBy(() -> guard.checkBeforePublish("Chào hỏi bằng ký hiệu",
                "hướng dẫn người mới chào hỏi bằng ngôn ngữ ký hiệu", null))
                .isInstanceOf(AppException.class)
                .hasMessage("Viết lại bài 9");
    }

    @Test
    void moderationRejected_blocked() {
        when(ai.moderate(anyString(), anyString())).thenReturn(new ModerationResult(false, "Quảng cáo", true));

        assertAppError(() -> guard.checkBeforePublish("Mua ngay", "giảm giá", null), ErrorCode.BLOG_REJECTED);
    }

    @Test
    void moderationUnavailable_failClosed() {
        when(ai.moderate(anyString(), anyString())).thenThrow(new AppException(ErrorCode.MODERATION_ERROR));

        assertAppError(() -> guard.checkBeforePublish("t", "c", null), ErrorCode.MODERATION_ERROR);
    }

    @Test
    void semanticCandidates_rankedBySimilarityAndUnrelatedDropped() {
        when(blogRepository.findDuplicateCandidates(isNull(), any())).thenReturn(List.of(
                blog(1L, "Thời tiết hôm nay", "trời mưa rất to ở thành phố"),
                blog(2L, "Học bảng chữ cái ký hiệu", "bảng chữ cái ngôn ngữ ký hiệu việt nam cơ bản"),
                blog(3L, "Bảng chữ cái", "học bảng chữ cái ký hiệu cho người mới bắt đầu")));

        List<ExistingBlog> picked = guard.semanticCandidates("Bảng chữ cái ký hiệu",
                "học bảng chữ cái ngôn ngữ ký hiệu cho người mới", null);

        assertThat(picked).extracting(ExistingBlog::id).doesNotContain(1L).contains(2L, 3L);
    }
}
