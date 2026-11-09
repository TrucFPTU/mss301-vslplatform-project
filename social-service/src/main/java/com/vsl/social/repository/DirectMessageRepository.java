package com.vsl.social.repository;

import com.vsl.social.entity.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    String BETWEEN = "((m.senderId = :a AND m.recipientId = :b) OR (m.senderId = :b AND m.recipientId = :a))";

    /** Tin giữa a và b có id < beforeId, mới nhất trước (đọc ngược lịch sử). */
    @Query("SELECT m FROM DirectMessage m WHERE " + BETWEEN + " AND m.id < :beforeId ORDER BY m.id DESC")
    List<DirectMessage> findThreadBefore(@Param("a") Long a, @Param("b") Long b,
                                         @Param("beforeId") Long beforeId, Pageable pageable);

    /** Tin giữa a và b có id > afterId, cũ nhất trước (FE hỏi tin mới định kỳ). */
    @Query("SELECT m FROM DirectMessage m WHERE " + BETWEEN + " AND m.id > :afterId ORDER BY m.id ASC")
    List<DirectMessage> findThreadAfter(@Param("a") Long a, @Param("b") Long b,
                                        @Param("afterId") Long afterId, Pageable pageable);

    /** Id tin cuối cùng của từng cuộc trò chuyện của userId, cuộc mới nhất trước. */
    @Query(value = "SELECT MAX(id) FROM direct_messages WHERE sender_id = :userId OR recipient_id = :userId "
            + "GROUP BY CASE WHEN sender_id = :userId THEN recipient_id ELSE sender_id END "
            + "ORDER BY MAX(id) DESC LIMIT :limit", nativeQuery = true)
    List<Long> findLastMessageIdPerConversation(@Param("userId") Long userId, @Param("limit") int limit);

    /** [người gửi, số tin chưa đọc] gửi tới userId. */
    @Query("SELECT m.senderId, COUNT(m) FROM DirectMessage m WHERE m.recipientId = :userId AND m.readAt IS NULL "
            + "GROUP BY m.senderId")
    List<Object[]> countUnreadBySender(@Param("userId") Long userId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    long countBySenderIdAndCreatedAtAfter(Long senderId, Instant after);

    @Modifying
    @Query("UPDATE DirectMessage m SET m.readAt = :now "
            + "WHERE m.recipientId = :me AND m.senderId = :other AND m.readAt IS NULL")
    int markRead(@Param("me") Long me, @Param("other") Long other, @Param("now") Instant now);
}
