package com.vsl.learning.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả chấm điểm do ai-service trả về (POST /api/ai/evaluate).
 * learning-service trả lại nguyên kết quả này cho client sau khi lưu lịch sử.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationResponse {

    public static final String STATUS_CORRECT = "CORRECT";

    /** CORRECT | ALMOST_CORRECT | INCORRECT */
    private String status;

    private String message;

    /** Xác suất model dự đoán đúng từ cần luyện (0.0 - 100.0). */
    private double confidence;

    /** Class ID được model dự đoán cao nhất. */
    private int predictedId;

    /** Thứ hạng của expectedId (1 = cao nhất). */
    private int rank;
}
