package com.vsl.social.repository;

import com.vsl.social.entity.BlogLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BlogLikeRepository extends JpaRepository<BlogLike, Long> {

    Optional<BlogLike> findByBlogIdAndUserId(Long blogId, Long userId);

    boolean existsByBlogIdAndUserId(Long blogId, Long userId);

    /** Những người đã like bài. */
    Page<BlogLike> findByBlogId(Long blogId, Pageable pageable);

    /** Trong các bài blogIds, bài nào userId đã like - tính likedByMe cho cả trang một lần. */
    @Query("SELECT l.blog.id FROM BlogLike l WHERE l.userId = :userId AND l.blog.id IN :blogIds")
    List<Long> findLikedBlogIds(@Param("userId") Long userId, @Param("blogIds") Collection<Long> blogIds);
}
