package com.vsl.social.repository;

import com.vsl.social.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    Optional<UserFollow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** Những người đang theo dõi followeeId. */
    Page<UserFollow> findByFolloweeId(Long followeeId, Pageable pageable);

    /** Những người mà followerId đang theo dõi. */
    Page<UserFollow> findByFollowerId(Long followerId, Pageable pageable);

    long countByFolloweeId(Long followeeId);

    long countByFollowerId(Long followerId);
}
