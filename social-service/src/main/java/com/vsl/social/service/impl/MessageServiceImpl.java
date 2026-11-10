package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.ChatThreadResponse;
import com.vsl.social.dto.ConversationResponse;
import com.vsl.social.dto.MessageResponse;
import com.vsl.social.dto.UserBriefResponse;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import com.vsl.social.entity.DirectMessage;
import com.vsl.social.repository.DirectMessageRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.BlockService;
import com.vsl.social.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tin nhắn riêng (không có ở monolith). Không dùng WebSocket: FE hỏi tin mới định kỳ bằng after=<id tin cuối>.
 * Không kiểm duyệt nội dung bằng AI (chỉ chặn rỗng / quá dài / gửi quá nhanh).
 * Một trong hai người chặn người kia -> không ai gửi được (vẫn đọc lại lịch sử cũ).
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    static final int MAX_CONVERSATIONS = 100;
    static final int MAX_PAGE = 100;
    /** Chống spam: tối đa RATE_LIMIT tin / RATE_WINDOW cho mỗi người gửi. */
    static final int RATE_LIMIT = 20;
    static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    private final DirectMessageRepository messageRepository;
    private final UserFollowRepository followRepository;
    private final IdentityClient identityClient;
    private final BlockService blockService;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations(Long userId) {
        List<Long> lastIds = messageRepository.findLastMessageIdPerConversation(userId, MAX_CONVERSATIONS);
        if (lastIds.isEmpty()) {
            return List.of();
        }
        List<DirectMessage> lasts = new ArrayList<>(messageRepository.findAllById(lastIds));
        lasts.sort(Comparator.comparing(DirectMessage::getId).reversed());
        List<Long> partners = lasts.stream().map(m -> partnerOf(m, userId)).toList();

        Map<Long, Long> unread = new HashMap<>();
        for (Object[] row : messageRepository.countUnreadBySender(userId)) {
            unread.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        Set<Long> linked = new HashSet<>(followRepository.findFolloweeIdsAmong(userId, partners));
        linked.addAll(followRepository.findFollowerIdsAmong(userId, partners));
        Set<Long> hidden = blockService.hiddenUserIds(userId);
        Set<Long> blockedByMe = new HashSet<>();
        for (Long p : partners) {
            if (hidden.contains(p) && blockService.hasBlocked(userId, p)) {
                blockedByMe.add(p);
            }
        }
        Map<Long, UserSummary> users = identityClient.getSummaries(partners);

        return lasts.stream().map(m -> {
            Long p = partnerOf(m, userId);
            UserSummary s = users.get(p);
            boolean deleted = s != null && s.deleted();
            return new ConversationResponse(UserBriefResponse.of(p, s), MessageResponse.of(m, userId),
                    unread.getOrDefault(p, 0L), linked.contains(p) && !hidden.contains(p) && !deleted,
                    blockedByMe.contains(p));
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatThreadResponse thread(Long userId, Long partnerId, Long before, Long after, int size) {
        requireNotSelf(userId, partnerId);
        int limit = Math.clamp(size, 1, MAX_PAGE);
        List<DirectMessage> page;
        boolean hasMore;
        if (after != null) {
            page = messageRepository.findThreadAfter(userId, partnerId, after, PageRequest.of(0, limit));
            hasMore = false; // chỉ dùng để hỏi tin mới; tin cũ hơn FE đã có
        } else {
            List<DirectMessage> newestFirst = messageRepository.findThreadBefore(userId, partnerId,
                    before != null ? before : Long.MAX_VALUE, PageRequest.of(0, limit + 1));
            hasMore = newestFirst.size() > limit;
            page = new ArrayList<>(newestFirst.subList(0, Math.min(limit, newestFirst.size())));
            Collections.reverse(page);
        }
        UserSummary summary = identityClient.getSummary(partnerId).orElse(null);
        // Người không tồn tại và chưa từng nhắn -> 404 (identity lỗi thì exists() = true, không chặn nhầm).
        if (summary == null && page.isEmpty() && before == null && after == null && !identityClient.exists(partnerId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        boolean blocked = blockService.isBlockedBetween(userId, partnerId);
        boolean deleted = summary != null && summary.deleted();
        return new ChatThreadResponse(UserBriefResponse.of(partnerId, summary),
                !blocked && !deleted && canMessage(userId, partnerId),
                page.stream().map(m -> MessageResponse.of(m, userId)).toList(), hasMore,
                blocked && blockService.hasBlocked(userId, partnerId), blocked);
    }

    @Override
    @Transactional
    public MessageResponse send(Long userId, Long recipientId, String content) {
        requireNotSelf(userId, recipientId);
        String text = content == null ? "" : content.strip();
        if (text.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tin nhắn không được để trống");
        }
        if (text.length() > DirectMessage.MAX_LENGTH) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tin nhắn tối đa 1000 ký tự");
        }
        if (blockService.isBlockedBetween(userId, recipientId)) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }
        // Tài khoản đã xóa -> 404, xét TRƯỚC quan hệ theo dõi: saga xóa tài khoản gỡ hết theo dõi của người đó,
        // nên nếu xét sau thì người gửi nhận nhầm lỗi "phải theo dõi nhau". identity lỗi -> không chặn.
        if (identityClient.getSummary(recipientId).map(UserSummary::deleted).orElse(false)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        if (!canMessage(userId, recipientId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_ALLOWED);
        }
        if (messageRepository.countBySenderIdAndCreatedAtAfter(userId, Instant.now().minus(RATE_WINDOW)) >= RATE_LIMIT) {
            throw new AppException(ErrorCode.MESSAGE_RATE_LIMITED);
        }
        DirectMessage saved = messageRepository.save(DirectMessage.builder()
                .senderId(userId).recipientId(recipientId).content(text).build());
        return MessageResponse.of(saved, userId);
    }

    @Override
    @Transactional
    public int markRead(Long userId, Long partnerId) {
        requireNotSelf(userId, partnerId);
        return messageRepository.markRead(userId, partnerId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messageRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canMessage(Long userId, Long otherId) {
        if (userId == null || otherId == null || Objects.equals(userId, otherId)) {
            return false;
        }
        if (blockService.isBlockedBetween(userId, otherId)) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFolloweeId(userId, otherId)
                || followRepository.existsByFollowerIdAndFolloweeId(otherId, userId);
    }

    private static void requireNotSelf(Long userId, Long otherId) {
        if (Objects.equals(userId, otherId)) {
            throw new AppException(ErrorCode.CANNOT_MESSAGE_SELF);
        }
    }

    private static Long partnerOf(DirectMessage m, Long userId) {
        return m.getSenderId().equals(userId) ? m.getRecipientId() : m.getSenderId();
    }
}
