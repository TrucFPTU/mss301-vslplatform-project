package com.vsl.learning.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Kết quả GET social-service /api/blogs/public-stats. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteBlogStats(long totalPublishedBlogs) {
}
