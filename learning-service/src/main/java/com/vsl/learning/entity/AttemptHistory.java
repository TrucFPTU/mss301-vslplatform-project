package com.vsl.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempt_history", indexes = {
        @Index(name = "idx_attempt_history_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User thuoc identity-service -> chi luu id, KHONG @ManyToOne User. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "ai_predicted_code")
    private Long aiPredictedCode;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;
}
