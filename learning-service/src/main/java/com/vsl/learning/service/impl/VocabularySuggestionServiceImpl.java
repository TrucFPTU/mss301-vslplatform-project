package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.CreateVocabularySuggestionRequest;
import com.vsl.learning.dto.response.VocabularySuggestionResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.SuggestionStatus;
import com.vsl.learning.entity.VocabularySuggestion;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.repository.VocabularySuggestionRepository;
import com.vsl.learning.service.VocabularySuggestionService;
import com.vsl.common.text.VietnameseText;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VocabularySuggestionServiceImpl implements VocabularySuggestionService {

    private final VocabularySuggestionRepository suggestionRepository;
    private final VocabularyRepository vocabularyRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public VocabularySuggestionResponse submit(Long userId, CreateVocabularySuggestionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh mục"));

        String word = VietnameseText.normalize(request.getWord());

        // Chặn nếu từ vựng đã tồn tại - không phân biệt hoa/thường VÀ không phân biệt dấu, toàn hệ thống.
        if (vocabularyExists(word)) {
            throw new AppException(ErrorCode.VOCABULARY_ALREADY_EXISTS);
        }

        // Chặn trùng với đề xuất PENDING khác (của bất kỳ ai) - tránh lặp hàng đợi duyệt của admin.
        if (isPendingDuplicate(word, null)) {
            throw new AppException(ErrorCode.VOCABULARY_SUGGESTION_ALREADY_PENDING);
        }

        VocabularySuggestion saved = suggestionRepository.save(VocabularySuggestion.builder()
                .category(category)
                .requesterId(userId)
                .word(word)
                .description(request.getDescription())
                .status(SuggestionStatus.PENDING)
                .build());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularySuggestionResponse> getMine(Long userId) {
        return suggestionRepository.findMineWithDetails(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public VocabularySuggestionResponse update(Long userId, Long id, CreateVocabularySuggestionRequest request) {
        VocabularySuggestion s = getOwnedPendingOrThrow(userId, id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh mục"));

        String word = VietnameseText.normalize(request.getWord());

        if (vocabularyExists(word)) {
            throw new AppException(ErrorCode.VOCABULARY_ALREADY_EXISTS);
        }

        // Chặn trùng với đề xuất PENDING khác (bỏ qua chính đề xuất đang sửa).
        if (isPendingDuplicate(word, id)) {
            throw new AppException(ErrorCode.VOCABULARY_SUGGESTION_ALREADY_PENDING);
        }

        s.setCategory(category);
        s.setWord(word);
        s.setDescription(request.getDescription());
        return toResponse(suggestionRepository.save(s));
    }

    @Override
    public void withdraw(Long userId, Long id) {
        VocabularySuggestion s = getOwnedPendingOrThrow(userId, id);
        suggestionRepository.delete(s);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VocabularySuggestionResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(Paging.safePage(page), Paging.safeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return Paging.toPageResponse(suggestionRepository.findAllWithDetails(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return suggestionRepository.countByStatus(SuggestionStatus.PENDING);
    }

    @Override
    public void markReviewed(Long id) {
        VocabularySuggestion s = suggestionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_SUGGESTION_NOT_FOUND));
        s.setStatus(SuggestionStatus.REVIEWED);
        s.setReviewedAt(LocalDateTime.now());
        suggestionRepository.save(s);
    }

    /**
     * Lấy đề xuất theo id, đảm bảo thuộc về {@code userId} và còn PENDING.
     * Ném VOCABULARY_SUGGESTION_NOT_FOUND nếu không có, FORBIDDEN nếu không phải chủ,
     * VOCABULARY_SUGGESTION_NOT_PENDING nếu đã được xem xét.
     */
    private VocabularySuggestion getOwnedPendingOrThrow(Long userId, Long id) {
        VocabularySuggestion s = suggestionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_SUGGESTION_NOT_FOUND));
        if (s.getRequesterId() == null || !s.getRequesterId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không phải người tạo đề xuất này");
        }
        if (s.getStatus() != SuggestionStatus.PENDING) {
            throw new AppException(ErrorCode.VOCABULARY_SUGGESTION_NOT_PENDING);
        }
        return s;
    }

    /** Từ đã tồn tại trong kho từ vựng chưa (so khớp bỏ dấu + hoa/thường). */
    private boolean vocabularyExists(String word) {
        String folded = VietnameseText.fold(word);
        return vocabularyRepository.findAllWords().stream()
                .anyMatch(w -> VietnameseText.fold(w).equals(folded));
    }

    /** Có đề xuất PENDING nào trùng từ này không (bỏ qua id đang sửa nếu có). */
    private boolean isPendingDuplicate(String word, Long excludeId) {
        String folded = VietnameseText.fold(word);
        return suggestionRepository.findByStatus(SuggestionStatus.PENDING).stream()
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .anyMatch(s -> VietnameseText.fold(s.getWord()).equals(folded));
    }

    private VocabularySuggestionResponse toResponse(VocabularySuggestion s) {
        Category c = s.getCategory();
        return VocabularySuggestionResponse.builder()
                .id(s.getId())
                .word(s.getWord())
                .description(s.getDescription())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .reviewedAt(s.getReviewedAt())
                .categoryId(c != null ? c.getId() : null)
                .categoryName(c != null ? c.getName() : null)
                .requesterId(s.getRequesterId())
                .build();
    }
}
