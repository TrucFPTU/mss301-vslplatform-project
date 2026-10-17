package com.vsl.learning.repository.projection;

public interface CategoryPracticeProjection {
    Long getCategoryId();
    String getCategoryName();
    Long getTotalVocabularies();
    Long getTotalAttempts();
    Long getCorrectAttempts();
}
