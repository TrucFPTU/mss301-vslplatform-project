package com.vsl.social.service.impl;

import com.vsl.social.service.BlockService;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.ChatThreadResponse;
import com.vsl.social.dto.ConversationResponse;
import com.vsl.social.dto.MessageResponse;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import com.vsl.social.entity.DirectMessage;
import com.vsl.social.repository.DirectMessageRepository;
import com.vsl.social.repository.UserFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    static final Long A = 1L;
    static final Long B = 2L;
    static final Long C = 3L;

    @Mock
    DirectMessageRepository messageRepository;
    @Mock
    UserFollowRepository followRepository;
    @Mock
    IdentityClient identityClient;
    @Mock
    BlockService blockService;
    @InjectMocks
    MessageServiceImpl service;

    private static DirectMessage msg(long id, Long from, Long to, String text) {
        DirectMessage m = DirectMessage.builder().senderId(from).recipientId(to).content(text).build();
        m.setId(id);
        m.setCreatedAt(Instant.now());
        return m;
    }

    private void follows(Long follower, Long followee) {
        when(followRepository.existsByFollowerIdAndFolloweeId(follower, followee)).thenReturn(true);
    }

    // ---------------- Quyền nhắn ----------------

    @Test
    void canMessage_whenEitherFollowsTheOther() {
        follows(A, B); // chỉ A theo dõi B
        assertThat(service.canMessage(B, A)).isTrue(); // B nhắn được cho A
        assertThat(service.canMessage(A, B)).isTrue(); // A cũng nhắn được cho B
        assertThat(service.canMessage(A, C)).isFalse();
        assertThat(service.canMessage(A, A)).isFalse();
    }

    @Test
    void send_followedByRecipient_savesTrimmedMessage() {
        follows(A, B);
        when(messageRepository.save(any())).thenAnswer(inv -> {
            DirectMessage m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MessageResponse res = service.send(B, A, "  chào bạn  ");

        ArgumentCaptor<DirectMessage> saved = ArgumentCaptor.forClass(DirectMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().getSenderId()).isEqualTo(B);
        assertThat(saved.getValue().getRecipientId()).isEqualTo(A);
        assertThat(saved.getValue().getContent()).isEqualTo("chào bạn");
        assertThat(res.mine()).isTrue();
    }

    @Test
    void blocked_cannotMessageEvenWhenFollowing() {
        follows(A, B);
        when(blockService.isBlockedBetween(A, B)).thenReturn(true);
        when(blockService.isBlockedBetween(B, A)).thenReturn(true);

        assertThat(service.canMessage(A, B)).isFalse();
        assertThat(service.canMessage(B, A)).isFalse();
        assertAppError(() -> service.send(B, A, "hi"), ErrorCode.USER_BLOCKED);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void thread_blockedByMe_flagged_historyStillReadable() {
        follows(A, B);
        when(blockService.isBlockedBetween(A, B)).thenReturn(true);
        when(blockService.hasBlocked(A, B)).thenReturn(true);
        when(messageRepository.findThreadBefore(eq(A), eq(B), anyLong(), any()))
                .thenReturn(new ArrayList<>(List.of(msg(1, B, A, "cũ"))));
        when(identityClient.getSummary(B)).thenReturn(java.util.Optional.of(new UserSummary(B, "bob", "Bob", null)));

        ChatThreadResponse res = service.thread(A, B, null, null, 30);

        assertThat(res.messages()).hasSize(1);
        assertThat(res.canMessage()).isFalse();
        assertThat(res.blockedByMe()).isTrue();
        assertThat(res.blocked()).isTrue();
    }

    @Test
    void send_toDeletedAccount_notFound_evenIfStillFollowing() {
        follows(A, B);
        when(identityClient.getSummary(B)).thenReturn(Optional.of(new UserSummary(B, "deleted_2", "Người dùng đã xóa", null, true)));

        assertAppError(() -> service.send(A, B, "còn đó không?"), ErrorCode.USER_NOT_FOUND);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void thread_withDeletedAccount_readOnly() {
        follows(A, B);
        when(messageRepository.findThreadBefore(eq(A), eq(B), anyLong(), any()))
                .thenReturn(new ArrayList<>(List.of(msg(1, A, B, "chào"))));
        when(identityClient.getSummary(B)).thenReturn(Optional.of(new UserSummary(B, "deleted_2", "Người dùng đã xóa", null, true)));

        ChatThreadResponse res = service.thread(A, B, null, null, 30);

        assertThat(res.messages()).hasSize(1);
        assertThat(res.canMessage()).isFalse();
        assertThat(res.partner().deleted()).isTrue();
    }

    @Test
    void send_1000CharsAfterTrim_ok() {
        follows(A, B);
        when(messageRepository.save(any())).thenAnswer(i -> { DirectMessage m = i.getArgument(0); m.setId(5L); return m; });

        assertThat(service.send(A, B, "  " + "x".repeat(1000) + "\n").content()).hasSize(1000);
    }

    @Test
    void send_stranger_forbidden() {
        assertAppError(() -> service.send(A, C, "xin chào"), ErrorCode.MESSAGE_NOT_ALLOWED);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_self_rejected() {
        assertAppError(() -> service.send(A, A, "xin chào"), ErrorCode.CANNOT_MESSAGE_SELF);
    }

    @Test
    void send_blankOrTooLong_rejected() {
        follows(A, B);
        assertAppError(() -> service.send(A, B, "   "), ErrorCode.VALIDATION_ERROR);
        assertAppError(() -> service.send(A, B, "x".repeat(DirectMessage.MAX_LENGTH + 1)), ErrorCode.VALIDATION_ERROR);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_tooFast_rateLimited() {
        follows(A, B);
        when(messageRepository.countBySenderIdAndCreatedAtAfter(eq(A), any()))
                .thenReturn((long) MessageServiceImpl.RATE_LIMIT);

        assertAppError(() -> service.send(A, B, "spam"), ErrorCode.MESSAGE_RATE_LIMITED);
        verify(messageRepository, never()).save(any());
    }

    // ---------------- Lịch sử ----------------

    @Test
    void thread_returnsOldestFirstWithHasMore() {
        follows(A, B);
        when(identityClient.getSummary(B)).thenReturn(Optional.of(new UserSummary(B, "bob", "Bob", null)));
        // size = 2 -> repo được hỏi 3 tin (mới nhất trước) để biết còn tin cũ hơn không
        when(messageRepository.findThreadBefore(eq(A), eq(B), eq(Long.MAX_VALUE), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(msg(7, B, A, "c"), msg(6, A, B, "b"), msg(5, B, A, "a"))));

        ChatThreadResponse res = service.thread(A, B, null, null, 2);

        assertThat(res.messages()).extracting(MessageResponse::id).containsExactly(6L, 7L);
        assertThat(res.messages().get(0).mine()).isTrue();
        assertThat(res.hasMore()).isTrue();
        assertThat(res.canMessage()).isTrue();
        assertThat(res.partner().displayName()).isEqualTo("Bob");
    }

    @Test
    void thread_afterId_returnsOnlyNewer() {
        when(identityClient.getSummary(B)).thenReturn(Optional.of(new UserSummary(B, "bob", "Bob", null)));
        when(messageRepository.findThreadAfter(eq(A), eq(B), eq(6L), any(Pageable.class)))
                .thenReturn(List.of(msg(7, B, A, "mới")));

        ChatThreadResponse res = service.thread(A, B, null, 6L, 30);

        assertThat(res.messages()).extracting(MessageResponse::id).containsExactly(7L);
        assertThat(res.hasMore()).isFalse();
        assertThat(res.canMessage()).isFalse(); // đã bỏ theo dõi nhau: vẫn xem được, không gửi được
    }

    @Test
    void thread_unknownUserWithoutHistory_notFound() {
        when(identityClient.getSummary(99L)).thenReturn(Optional.empty());
        when(identityClient.exists(99L)).thenReturn(false);
        when(messageRepository.findThreadBefore(anyLong(), anyLong(), anyLong(), any())).thenReturn(new ArrayList<>());

        assertAppError(() -> service.thread(A, 99L, null, null, 30), ErrorCode.USER_NOT_FOUND);
    }

    // ---------------- Hộp thư ----------------

    @Test
    void conversations_partnerLastMessageUnreadAndPermission() {
        when(messageRepository.findLastMessageIdPerConversation(A, MessageServiceImpl.MAX_CONVERSATIONS))
                .thenReturn(List.of(9L, 4L));
        // findAllById không bảo đảm thứ tự -> service tự sắp mới nhất trước
        when(messageRepository.findAllById(List.of(9L, 4L)))
                .thenReturn(List.of(msg(4, A, C, "cũ"), msg(9, B, A, "mới")));
        when(messageRepository.countUnreadBySender(A)).thenReturn(List.<Object[]>of(new Object[]{B, 2L}));
        when(followRepository.findFolloweeIdsAmong(A, List.of(B, C))).thenReturn(List.of());
        when(followRepository.findFollowerIdsAmong(A, List.of(B, C))).thenReturn(List.of(B));
        when(identityClient.getSummaries(List.of(B, C))).thenReturn(Map.of(
                B, new UserSummary(B, "bob", "Bob", null), C, new UserSummary(C, "carol", null, null)));

        List<ConversationResponse> res = service.conversations(A);

        assertThat(res).extracting(c -> c.partner().userId()).containsExactly(B, C);
        assertThat(res.get(0).unreadCount()).isEqualTo(2);
        assertThat(res.get(0).canMessage()).isTrue();
        assertThat(res.get(0).lastMessage().mine()).isFalse();
        assertThat(res.get(1).unreadCount()).isZero();
        assertThat(res.get(1).canMessage()).isFalse();
        assertThat(res.get(1).partner().displayName()).isEqualTo("carol");
    }

    @Test
    void markRead_onlyMessagesFromThatPartnerToMe() {
        when(messageRepository.markRead(eq(A), eq(B), any())).thenReturn(3);

        assertThat(service.markRead(A, B)).isEqualTo(3);
        verify(messageRepository).markRead(eq(A), eq(B), any());
    }
}
