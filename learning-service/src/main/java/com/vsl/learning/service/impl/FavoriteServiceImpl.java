package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.repository.UserFavoriteRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final VocabularyRepository vocabularyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyResponse> getMine(Long userId) {
        return favoriteRepository.findAllWithVocabularyByUserId(userId).stream()
                .map(f -> VocabularyServiceImpl.toResponse(f.getVocabulary()))
                .toList();
    }

    @Override
    public void add(Long userId, Long vocabularyId) {
        if (!vocabularyRepository.existsById(vocabularyId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng");
        }
        favoriteRepository.insertIfAbsent(userId, vocabularyId, LocalDateTime.now());
    }

    @Override
    public void remove(Long userId, Long vocabularyId) {
        favoriteRepository.deleteByUserIdAndVocabularyId(userId, vocabularyId);
    }
}
