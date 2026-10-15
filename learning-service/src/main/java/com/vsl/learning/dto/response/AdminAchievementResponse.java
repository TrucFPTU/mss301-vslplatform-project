package com.vsl.learning.dto.response;

import com.vsl.learning.entity.Achievement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAchievementResponse {
    private Long id;
    private String key;
    private String name;
    private String description;
    private String iconKey;
    private Long unlockedUserCount;

    public static AdminAchievementResponse from(Achievement a, Long unlockCount) {
        return AdminAchievementResponse.builder()
                .id(a.getId())
                .key(a.getKey())
                .name(a.getName())
                .description(a.getDescription())
                .iconKey(a.getIconKey())
                .unlockedUserCount(unlockCount != null ? unlockCount : 0L)
                .build();
    }
}
