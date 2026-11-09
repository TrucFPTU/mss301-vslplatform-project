package com.vsl.social.service;

import com.vsl.social.dto.ChatThreadResponse;
import com.vsl.social.dto.ConversationResponse;
import com.vsl.social.dto.MessageResponse;

import java.util.List;

/**
 * Tin nhắn riêng. Được gửi khi một trong hai người đang theo dõi người kia;
 * lịch sử đã có thì luôn xem lại được (kể cả khi đã bỏ theo dõi).
 */
public interface MessageService {

    /** Hộp thư: các cuộc trò chuyện, mới nhất trước (tối đa 100). */
    List<ConversationResponse> conversations(Long userId);

    /** Lịch sử với partnerId: before = lấy tin cũ hơn id đó; after = chỉ lấy tin mới hơn id đó (hỏi định kỳ). */
    ChatThreadResponse thread(Long userId, Long partnerId, Long before, Long after, int size);

    MessageResponse send(Long userId, Long recipientId, String content);

    /** Đánh dấu đã đọc mọi tin partnerId gửi cho userId. Trả số tin vừa đánh dấu. */
    int markRead(Long userId, Long partnerId);

    /** Tổng số tin chưa đọc của userId. */
    long unreadCount(Long userId);

    /** userId và otherId có được nhắn cho nhau không (một trong hai đang theo dõi người kia). */
    boolean canMessage(Long userId, Long otherId);
}
