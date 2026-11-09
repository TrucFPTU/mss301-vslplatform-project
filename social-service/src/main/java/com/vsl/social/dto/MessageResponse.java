package com.vsl.social.dto;

import com.vsl.social.entity.DirectMessage;

import java.time.Instant;

/** Một tin nhắn; mine = do người đang xem gửi. */
public record MessageResponse(
        Long id,
        Long senderId,
        Long recipientId,
        String content,
        boolean mine,
        Instant createdAt,
        Instant readAt
) {
    public static MessageResponse of(DirectMessage m, Long viewerId) {
        return new MessageResponse(m.getId(), m.getSenderId(), m.getRecipientId(), m.getContent(),
                m.getSenderId().equals(viewerId), m.getCreatedAt(), m.getReadAt());
    }
}
