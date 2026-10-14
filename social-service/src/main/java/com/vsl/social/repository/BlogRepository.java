package com.vsl.social.repository;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByStatus(BlogStatus status, Pageable pageable);

    Page<Blog> findByStatusAndTitleContainingIgnoreCase(BlogStatus status, String keyword, Pageable pageable);

    Page<Blog> findByAuthorId(Long authorId, Pageable pageable);

    Page<Blog> findByAuthorIdAndStatus(Long authorId, BlogStatus status, Pageable pageable);

    // Cộng dồn trực tiếp trong DB để không mất lượt khi nhiều request cùng lúc.
    // clearAutomatically: entity đã load trước đó sẽ bị detach — đọc lại nếu cần số mới.

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Blog b set b.likeCount = b.likeCount + :delta where b.id = :id")
    int addLikeCount(@Param("id") Long id, @Param("delta") long delta);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Blog b set b.commentCount = b.commentCount + :delta where b.id = :id")
    int addCommentCount(@Param("id") Long id, @Param("delta") long delta);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Blog b set b.shareCount = b.shareCount + :delta where b.id = :id")
    int addShareCount(@Param("id") Long id, @Param("delta") long delta);
}
