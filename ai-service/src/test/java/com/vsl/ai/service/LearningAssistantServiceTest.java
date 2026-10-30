package com.vsl.ai.service;

import com.vsl.ai.client.LearningServiceClient;
import com.vsl.ai.dto.AssistantDtos.Card;
import com.vsl.ai.dto.AssistantDtos.CategoryItem;
import com.vsl.ai.dto.AssistantDtos.ChatMessage;
import com.vsl.ai.dto.AssistantDtos.ChatRequest;
import com.vsl.ai.dto.AssistantDtos.ChatResponse;
import com.vsl.ai.dto.AssistantDtos.LearningContext;
import com.vsl.ai.dto.AssistantDtos.Recommendation;
import com.vsl.ai.dto.AssistantDtos.Stats;
import com.vsl.ai.dto.AssistantDtos.VocabularyItem;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningAssistantServiceTest {

    private static final String AUTH = "Bearer t";

    private final LearningServiceClient learningClient = mock(LearningServiceClient.class);

    private static final LearningContext CONTEXT = new LearningContext(2, 3, 2,
            new Stats(10, 6, 1, 2, 60.0, 50, 3, 5, List.of(false, false, true, true, false, true, true)),
            List.of(new CategoryItem(1L, "Chào hỏi", 2, 1, 1), new CategoryItem(2L, "Gia đình", 1, 0, 0)),
            List.of(new VocabularyItem(11L, "xin chào", 1L, "Chào hỏi", "http://img/11", true, "LEARNED"),
                    new VocabularyItem(12L, "tạm biệt", 1L, "Chào hỏi", null, true, "LEARNING"),
                    new VocabularyItem(13L, "bố", 2L, "Gia đình", null, false, "NEW")),
            List.of(new Recommendation(12L, "tạm biệt", "Bạn đã luyện nhưng chưa ký đúng — nên ôn lại")));

    /** GeminiClient thật (để readTree / parseCandidateJson chạy thật), chỉ thay phần gọi mạng. */
    private static GeminiClient gemini(String modelJson, boolean enabled) {
        return new GeminiClient(enabled ? "key" : "", "m", List.of(), true) {
            @Override
            public String generateJson(String prompt, double temperature, ErrorCode errorCode) {
                String quoted = "\"" + modelJson.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quoted + "}]}}]}";
            }
        };
    }

    private static ChatRequest ask(String q) {
        return new ChatRequest(List.of(new ChatMessage("user", q)));
    }

    @Test
    @DisplayName("Trả câu trả lời + thẻ; id Gemini bịa / trùng bị bỏ, thẻ chủ đề kèm số đã thuộc")
    void chat_BuildsCardsFromRealIdsOnly() {
        when(learningClient.getAssistantContext(AUTH)).thenReturn(CONTEXT);
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(
                gemini("{\"reply\":\"Bạn nên ôn “tạm biệt”.\",\"vocabularyIds\":[12, 999, \"12\", \"11\"],\"categoryIds\":[1, 77]}", true),
                learningClient);

        ChatResponse res = service.chat(1L, AUTH, ask("Mình nên học gì?"));

        assertThat(res.reply()).isEqualTo("Bạn nên ôn “tạm biệt”.");
        assertThat(res.cards()).extracting(Card::type, Card::id)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("VOCABULARY", 12L),
                        org.assertj.core.groups.Tuple.tuple("VOCABULARY", 11L),
                        org.assertj.core.groups.Tuple.tuple("CATEGORY", 1L));
        assertThat(res.cards().get(0).subtitle()).isEqualTo("Chào hỏi · đang học");
        assertThat(res.cards().get(1).imageUrl()).isEqualTo("http://img/11");
        assertThat(res.cards().get(2).subtitle()).isEqualTo("Đã thuộc 1/2 từ");
    }

    @Test
    @DisplayName("Prompt chứa số liệu thật, gợi ý và câu hỏi nằm trong vùng phân cách")
    void chat_PromptContainsContextAndDelimitedQuestion() {
        when(learningClient.getAssistantContext(AUTH)).thenReturn(CONTEXT);
        GeminiClient g = mock(GeminiClient.class);
        when(g.isEnabled()).thenReturn(true);
        when(g.generateJson(anyString(), anyDouble(), eq(ErrorCode.AI_EXTERNAL_ERROR))).thenThrow(new AppException(ErrorCode.AI_EXTERNAL_ERROR));
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(g, learningClient);

        assertThatThrownBy(() -> service.chat(1L, AUTH, ask("Hệ thống có bao nhiêu bài?")))
                .isInstanceOf(AppException.class);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(g).generateJson(prompt.capture(), anyDouble(), eq(ErrorCode.AI_EXTERNAL_ERROR));
        assertThat(prompt.getValue())
                .contains("2 chủ đề, 3 từ vựng")
                .contains("đã thuộc 1/2 từ luyện được (50%), đang học 1 từ")
                .contains("7 ngày qua có luyện 4 ngày")
                .contains("12 | tạm biệt | Chào hỏi | đang học | có")
                .contains("12 | tạm biệt | Bạn đã luyện nhưng chưa ký đúng")
                .contains("<<<HỘI_THOẠI>>>\nNGƯỜI HỌC: Hệ thống có bao nhiêu bài?\n<<<HẾT_HỘI_THOẠI>>>");
    }

    @Test
    @DisplayName("Gemini chưa cấu hình -> 503 AI_EXTERNAL_ERROR, không gọi learning (FE tự trả lời bằng luật)")
    void chat_Disabled() {
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(gemini("{}", false), learningClient);

        assertThatThrownBy(() -> service.chat(1L, AUTH, ask("hi")))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_EXTERNAL_ERROR));
        verify(learningClient, never()).getAssistantContext(anyString());
    }

    @Test
    @DisplayName("Tin cuối không phải của người học -> 400")
    void chat_LastMessageMustBeUser() {
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(gemini("{}", true), learningClient);
        ChatRequest req = new ChatRequest(List.of(new ChatMessage("user", "a"), new ChatMessage("assistant", "b")));

        assertThatThrownBy(() -> service.chat(1L, AUTH, req))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    @DisplayName("Gemini trả reply rỗng -> 503")
    void chat_EmptyReply() {
        when(learningClient.getAssistantContext(AUTH)).thenReturn(CONTEXT);
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(gemini("{\"reply\":\"  \"}", true), learningClient);

        assertThatThrownBy(() -> service.chat(1L, AUTH, ask("hi"))).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Hỏi quá RATE_LIMIT lần/phút -> 429, người khác không bị ảnh hưởng")
    void chat_RateLimitedPerUser() {
        when(learningClient.getAssistantContext(AUTH)).thenReturn(CONTEXT);
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(gemini("{\"reply\":\"ok\"}", true), learningClient);
        for (int i = 0; i < LearningAssistantServiceImpl.RATE_LIMIT; i++) {
            service.chat(1L, AUTH, ask("q" + i));
        }

        assertThatThrownBy(() -> service.chat(1L, AUTH, ask("thêm")))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_ASSISTANT_RATE_LIMITED));
        assertThat(service.chat(2L, AUTH, ask("q")).reply()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Chỉ giữ 10 tin gần nhất, cắt tin quá dài")
    void trimHistory() {
        List<ChatMessage> msgs = new ArrayList<>();
        for (int i = 0; i < 14; i++) msgs.add(new ChatMessage(i % 2 == 0 ? "user" : "assistant", "m" + i));
        msgs.add(new ChatMessage("user", "x".repeat(1500)));

        List<ChatMessage> trimmed = LearningAssistantServiceImpl.trimHistory(msgs);

        assertThat(trimmed).hasSize(LearningAssistantServiceImpl.MAX_HISTORY);
        assertThat(trimmed.get(0).content()).isEqualTo("m5");
        assertThat(trimmed.get(9).content()).hasSize(LearningAssistantServiceImpl.MAX_MESSAGE_CHARS);
    }
}
