package com.vsl.learning.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AchievementResponse {
    private final Long id;
    private final String key;
    private final String name;
    private final String description;
    private final String iconKey;
    private final boolean unlocked;
    private final LocalDateTime unlockedAt;
}
