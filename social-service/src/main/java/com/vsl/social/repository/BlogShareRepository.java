package com.vsl.social.repository;

import com.vsl.social.entity.BlogShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogShareRepository extends JpaRepository<BlogShare, Long> {

    /**
     * Bài hiện trên trang cá nhân của userId: bài người đó tự đăng lên trang mình + bài bạn bè chia sẻ cho.
     * Sửa monolith: chỉ lấy bài còn PUBLISHED (bản cũ hiện cả bài đã bị gỡ / chuyển về nháp).
     */
    @EntityGraph(attributePaths = {"blog"})
    @Query("SELECT s FROM BlogShare s WHERE s.shareType = com.vsl.social.entity.ShareType.PROFILE "
            + "AND s.blog.status = com.vsl.social.entity.BlogStatus.PUBLISHED "
            + "AND (s.recipientId = :userId OR (s.recipientId IS NULL AND s.userId = :userId))")
    Page<BlogShare> findProfileShares(@Param("userId") Long userId, Pageable pageable);
}
