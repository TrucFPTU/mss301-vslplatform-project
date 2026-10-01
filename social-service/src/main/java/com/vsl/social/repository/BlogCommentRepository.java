package com.vsl.social.repository;

import com.vsl.social.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {

    Page<BlogComment> findByBlogId(Long blogId, Pageable pageable);
}
