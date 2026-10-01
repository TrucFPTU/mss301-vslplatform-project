package com.vsl.learning.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String imageUrl;
}
