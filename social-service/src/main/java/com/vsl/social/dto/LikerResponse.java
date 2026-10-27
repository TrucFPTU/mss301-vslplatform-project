package com.vsl.social.dto;

import java.time.Instant;

/** Một người đã like bài. */
public record LikerResponse(UserBriefResponse user, Instant likedAt) {
}
