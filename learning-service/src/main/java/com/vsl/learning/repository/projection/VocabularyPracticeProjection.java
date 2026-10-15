package com.vsl.learning.repository.projection;

public interface VocabularyPracticeProjection {
    Long getVocabularyId();
    String getWord();
    String getCategoryName();
    Long getAttemptCount();
    Long getCorrectCount();
}
