package com.vsl.learning.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VocabularyResponse {
    private final Long id;
    private final Long categoryId;
    private final String categoryName;
    private final String word;
    private final String description;
    private final String videoTutorialUrl;
    private final String imageUrl;
    private final Integer expectedId;
}
