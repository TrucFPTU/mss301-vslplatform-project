package com.vsl.ai.service;

import com.vsl.ai.dto.AssistantDtos.ChatRequest;
import com.vsl.ai.dto.AssistantDtos.ChatResponse;

/** Trợ lý học tập: trả lời câu hỏi của người học dựa trên dữ liệu thật của learning-service (Gemini diễn đạt). */
public interface LearningAssistantService {

    /**
     * @param authorization header Authorization của người học (chuyển tiếp cho learning-service)
     * @throws com.vsl.common.exception.AppException AI_EXTERNAL_ERROR khi Gemini tắt / lỗi - FE tự trả lời bằng luật
     */
    ChatResponse chat(Long userId, String authorization, ChatRequest request);
}
