package com.vsl.social.repository;

import com.vsl.social.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** Xóa mọi quan hệ chặn có userId ở một trong hai đầu (người dùng đã xóa tài khoản). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserBlock b where b.blockerId = :id or b.blockedId = :id")
    int deleteAllInvolving(@Param("id") Long id);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** Những người mà blockerId đã chặn. */
    Page<UserBlock> findByBlockerId(Long blockerId, Pageable pageable);

    /** a chặn b hoặc b chặn a. */
    @Query("SELECT COUNT(x) > 0 FROM UserBlock x WHERE (x.blockerId = :a AND x.blockedId = :b) "
            + "OR (x.blockerId = :b AND x.blockedId = :a)")
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    /** Mọi người có quan hệ chặn với userId (userId chặn họ hoặc họ chặn userId). */
    @Query("SELECT CASE WHEN x.blockerId = :userId THEN x.blockedId ELSE x.blockerId END FROM UserBlock x "
            + "WHERE x.blockerId = :userId OR x.blockedId = :userId")
    List<Long> findRelatedUserIds(@Param("userId") Long userId);
}
