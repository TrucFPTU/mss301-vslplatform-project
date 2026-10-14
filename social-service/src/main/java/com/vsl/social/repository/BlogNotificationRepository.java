package com.vsl.social.repository;

import com.vsl.social.entity.BlogNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogNotificationRepository extends JpaRepository<BlogNotification, Long> {

    Page<BlogNotification> findByRecipientId(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("update BlogNotification n set n.read = true where n.recipientId = :recipientId and n.read = false")
    int markAllRead(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("delete from BlogNotification n where n.blogId = :blogId")
    int deleteByBlogId(@Param("blogId") Long blogId);
}
