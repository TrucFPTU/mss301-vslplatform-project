package com.vsl.social.repository;

import com.vsl.social.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    Optional<UserFollow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** Những người đang theo dõi followeeId. */
    Page<UserFollow> findByFolloweeId(Long followeeId, Pageable pageable);

    /** Những người mà followerId đang theo dõi. */
    Page<UserFollow> findByFollowerId(Long followerId, Pageable pageable);

    long countByFolloweeId(Long followeeId);

    long countByFollowerId(Long followerId);

    /** [userId, số người theo dõi] cho nhiều người một lần. */
    @Query("SELECT f.followeeId, COUNT(f) FROM UserFollow f WHERE f.followeeId IN :ids GROUP BY f.followeeId")
    List<Object[]> countFollowersByIds(@Param("ids") Collection<Long> ids);

    /** [userId, số người đang theo dõi] cho nhiều người một lần. */
    @Query("SELECT f.followerId, COUNT(f) FROM UserFollow f WHERE f.followerId IN :ids GROUP BY f.followerId")
    List<Object[]> countFollowingByIds(@Param("ids") Collection<Long> ids);

    /** Trong ids, những người mà userId đang theo dõi. */
    @Query("SELECT f.followeeId FROM UserFollow f WHERE f.followerId = :userId AND f.followeeId IN :ids")
    List<Long> findFolloweeIdsAmong(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    /** Trong ids, những người đang theo dõi userId. */
    @Query("SELECT f.followerId FROM UserFollow f WHERE f.followeeId = :userId AND f.followerId IN :ids")
    List<Long> findFollowerIdsAmong(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    /** Bạn bè = hai người theo dõi nhau. Trả các follow (followerId = người bạn) của userId. */
    @Query(value = "SELECT f FROM UserFollow f WHERE f.followeeId = :userId AND EXISTS ("
            + "SELECT 1 FROM UserFollow f2 WHERE f2.followerId = :userId AND f2.followeeId = f.followerId)",
            countQuery = "SELECT COUNT(f) FROM UserFollow f WHERE f.followeeId = :userId AND EXISTS ("
            + "SELECT 1 FROM UserFollow f2 WHERE f2.followerId = :userId AND f2.followeeId = f.followerId)")
    Page<UserFollow> findFriends(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.followeeId = :userId AND EXISTS ("
            + "SELECT 1 FROM UserFollow f2 WHERE f2.followerId = :userId AND f2.followeeId = f.followerId)")
    long countFriends(@Param("userId") Long userId);
}
