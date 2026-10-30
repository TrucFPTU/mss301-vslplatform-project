package com.vsl.learning.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttemptResponse {
    private final Long attemptId;
    private final Long vocabularyId;
    private final String word;
    private final Long categoryId;
    private final String categoryName;
    private final Integer expectedId;
    private final Boolean isCorrect;
    private final Long aiPredictedCode;
    private final Double confidence;
    private final LocalDateTime attemptedAt;
}
