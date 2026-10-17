package com.vsl.learning.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "Số liệu luyện tập theo từng ngày phục vụ biểu đồ xu hướng (Trends)")
public class DailyActivityResponse {

    @Schema(description = "Ngày thống kê", example = "2026-10-14")
    private final LocalDate date;

    @Schema(description = "Tổng số lượt luyện tập trong ngày", example = "125")
    private final long totalAttempts;

    @Schema(description = "Số lượt luyện tập đúng", example = "98")
    private final long correctAttempts;

    @Schema(description = "Số lượt luyện tập sai", example = "27")
    private final long incorrectAttempts;

    @Schema(description = "Số học viên hoạt động trong ngày", example = "24")
    private final long activeLearners;

    @Schema(description = "Tỷ lệ làm đúng trong ngày (%)", example = "78.4")
    private final double accuracyRate;
}
