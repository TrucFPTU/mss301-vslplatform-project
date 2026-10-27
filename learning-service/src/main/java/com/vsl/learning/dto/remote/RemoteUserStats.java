package com.vsl.learning.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Kết quả GET identity-service /api/users/public-stats. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteUserStats(long totalUsers, long totalStudents) {
}
