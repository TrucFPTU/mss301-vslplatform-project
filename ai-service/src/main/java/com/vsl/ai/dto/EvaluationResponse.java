package com.vsl.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả chấm điểm ký hiệu mà ai-service trả về (POST /api/ai/evaluate).
 *
 * <pre>
 * {
 *   "status":      "CORRECT",
 *   "message":     "Chính xác! Bạn thực hiện ký hiệu rất tốt.",
 *   "confidence":  87.43,
 *   "predictedId": 42,
 *   "rank":        1
 * }
 * </pre>
 *
 * Field phải TRÙNG tên với {@code com.vsl.learning.dto.ai.EvaluationResponse}
 * vì learning-service deserialize JSON này.
 *
 * status:
 *   CORRECT        - rank 1 và confidence > 50%
 *   ALMOST_CORRECT - rank 2..5
 *   INCORRECT      - rank > 5
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluationResponse {

    private final String status;

    private final String message;

    /** Xác suất model dự đoán đúng từ cần luyện (0.0 - 100.0). */
    private final double confidence;

    /** Class ID (0-999) được model dự đoán cao nhất. */
    private final int predictedId;

    /** Thứ hạng của expectedId trong danh sách xác suất (1 = cao nhất). */
    private final int rank;
}
