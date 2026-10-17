package com.vsl.social.dto;

import com.vsl.social.repository.projection.CreatorStatProjection;
import lombok.Builder;

@Builder
public record TopCreatorResponse(
        Long authorId,
        long totalPosts,
        long publishedPosts,
        long totalLikesReceived
) {
    public static TopCreatorResponse from(CreatorStatProjection p) {
        return TopCreatorResponse.builder()
                .authorId(p.getAuthorId())
                .totalPosts(p.getTotalPosts() != null ? p.getTotalPosts() : 0L)
                .publishedPosts(p.getPublishedPosts() != null ? p.getPublishedPosts() : 0L)
                .totalLikesReceived(p.getTotalLikesReceived() != null ? p.getTotalLikesReceived() : 0L)
                .build();
    }
}
