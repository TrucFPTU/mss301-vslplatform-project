package com.vsl.learning.entity;

import jakarta.persistence.*;
import lombok.*;

/** Danh muc thanh tuu (seed san khi khoi dong, xem AchievementService). */
@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    private Long id;

    @Column(name = "achievement_key", nullable = false, unique = true, length = 50)
    private String key;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "icon_key", length = 50)
    private String iconKey;
}
