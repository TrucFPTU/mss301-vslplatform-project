package com.vsl.social.dto;

/** Một cuộc trò chuyện trong hộp thư: đối phương, tin cuối, số tin chưa đọc, còn được nhắn tiếp không. */
public record ConversationResponse(
        UserBriefResponse partner,
        MessageResponse lastMessage,
        long unreadCount,
        boolean canMessage
) {
}
