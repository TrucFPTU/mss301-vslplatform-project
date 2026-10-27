package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.text.VietnameseText;
import com.vsl.social.client.AiModerationClient;
import com.vsl.social.dto.remote.RemoteDtos.DuplicateCheckResult;
import com.vsl.social.dto.remote.RemoteDtos.ExistingBlog;
import com.vsl.social.dto.remote.RemoteDtos.ModerationResult;
import com.vsl.social.entity.Blog;
import com.vsl.social.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Các bước kiểm tra trước khi một bài được ĐĂNG CÔNG KHAI (bài nháp bỏ qua). Bê từ BlogServiceImpl monolith,
 * theo đúng thứ tự cũ (rẻ trước, gọi AI sau):
 * <ol>
 *   <li>trùng tiêu đề + nội dung với một bài TỪNG bị tố cáo -> chặn đăng lại nội dung vi phạm</li>
 *   <li>trùng y hệt (sau chuẩn hóa) tiêu đề + nội dung với một bài đã công khai</li>
 *   <li>trùng tiêu đề với một bài đã công khai</li>
 *   <li>trùng Ý (viết lại / đảo chữ) - AI</li>
 *   <li>kiểm duyệt nội dung - AI</li>
 * </ol>
 * excludeBlogId: bỏ qua chính bài đang sửa (null khi tạo mới).
 */
@Component
@RequiredArgsConstructor
public class BlogPublishGuard {

    /** Tối đa số bài gửi AI so trùng ngữ nghĩa (giới hạn chi phí / độ dài prompt) - như monolith. */
    static final int MAX_SEMANTIC_CANDIDATES = 20;
    /** Quét tối đa ngần này bài mới nhất để chọn ứng viên. */
    static final int CANDIDATE_POOL = 300;
    /** Độ trùng từ tối thiểu để đáng gửi AI so (bỏ các bài chỉ chung vài hư từ). */
    static final double MIN_SIMILARITY = 0.15;

    private final BlogRepository blogRepository;
    private final AiModerationClient aiModerationClient;

    public void checkBeforePublish(String title, String content, Long excludeBlogId) {
        String normalizedTitle = truncate(VietnameseText.fold(title));
        String fingerprint = VietnameseText.fingerprint(content);

        if (blogRepository.existsReportedWithContent(normalizedTitle, fingerprint, excludeBlogId)) {
            throw new AppException(ErrorCode.BLOG_CONTENT_ALREADY_REPORTED);
        }
        if (blogRepository.existsPublishedWithContent(normalizedTitle, fingerprint, excludeBlogId)) {
            throw new AppException(ErrorCode.BLOG_DUPLICATE_CONTENT);
        }
        if (!normalizedTitle.isBlank() && blogRepository.existsPublishedWithTitle(normalizedTitle, excludeBlogId)) {
            throw new AppException(ErrorCode.BLOG_DUPLICATE_TITLE);
        }

        List<ExistingBlog> candidates = semanticCandidates(title, content, excludeBlogId);
        if (!candidates.isEmpty()) {
            DuplicateCheckResult dup = aiModerationClient.checkDuplicate(title, content, candidates);
            if (dup.duplicate()) {
                throw new AppException(ErrorCode.BLOG_DUPLICATE_CONTENT_AI,
                        orDefault(dup.reason(), "AI phát hiện nội dung trùng ý với một bài đã có."));
            }
        }

        ModerationResult mod = aiModerationClient.moderate(title, content);
        if (!mod.allowed()) {
            throw new AppException(ErrorCode.BLOG_REJECTED,
                    orDefault(mod.reason(), "Nội dung không phù hợp với tiêu chuẩn cộng đồng."));
        }
    }

    /**
     * Chọn các bài gần giống nhất để gửi AI. Monolith chỉ lọc "có chung ít nhất 1 từ" - với tiếng Việt gần như
     * bài nào cũng chung hư từ ("la", "co", "cua"...), nên thực chất gửi 20 bài bất kỳ. Ở đây xếp hạng theo
     * độ trùng tập từ (Jaccard, bỏ từ < 3 ký tự) và chỉ lấy các bài đủ giống.
     */
    List<ExistingBlog> semanticCandidates(String title, String content, Long excludeBlogId) {
        Set<String> tokens = tokenize(title, content);
        if (tokens.isEmpty()) {
            return List.of();
        }
        record Scored(Blog blog, double score) {
        }
        return blogRepository.findDuplicateCandidates(excludeBlogId, PageRequest.of(0, CANDIDATE_POOL)).stream()
                .map(b -> new Scored(b, jaccard(tokens, tokenize(b.getTitle(), b.getContent()))))
                .filter(s -> s.score() >= MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(MAX_SEMANTIC_CANDIDATES)
                .map(s -> new ExistingBlog(s.blog().getId(), s.blog().getTitle(), s.blog().getContent()))
                .toList();
    }

    static Set<String> tokenize(String title, String content) {
        String folded = VietnameseText.fold((title == null ? "" : title) + " " + (content == null ? "" : content));
        return Arrays.stream(folded.split("[^\\p{L}\\p{N}]+"))
                .filter(w -> w.length() >= 3)
                .collect(Collectors.toCollection(HashSet::new));
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        long common = a.stream().filter(b::contains).count();
        return (double) common / (a.size() + b.size() - common);
    }

    private static String truncate(String s) {
        return s.length() > 255 ? s.substring(0, 255) : s;
    }

    private static String orDefault(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }
}
