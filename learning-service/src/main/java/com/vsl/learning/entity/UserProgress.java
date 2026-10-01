package com.vsl.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tien trinh hoc cua 1 user voi 1 tu vung.
 * UNIQUE (user_id, vocabulary_id): moi user chi co 1 dong cho moi tu vung.
 */
@Entity
@Table(name = "user_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_progress_user_vocab", columnNames = {"user_id", "vocabulary_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User thuoc identity-service -> chi luu id, KHONG @ManyToOne User. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_status")
    private LearningStatus learningStatus;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;
}
