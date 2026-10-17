package com.vsl.learning.repository.projection;

import java.time.LocalDateTime;

public interface AttemptTrendProjection {
    LocalDateTime getAttemptedAt();
    Boolean getIsCorrect();
    Long getUserId();
}
