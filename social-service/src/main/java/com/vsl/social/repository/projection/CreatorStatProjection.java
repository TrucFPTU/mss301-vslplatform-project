package com.vsl.social.repository.projection;

public interface CreatorStatProjection {
    Long getAuthorId();
    Long getTotalPosts();
    Long getPublishedPosts();
    Long getTotalLikesReceived();
}
