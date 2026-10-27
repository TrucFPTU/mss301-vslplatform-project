package com.vsl.social.repository;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    long countByStatus(BlogStatus status);

    // ---- Chặn đăng trùng (so trên dạng đã chuẩn hóa, có index) ----
    // Dùng (:excludeId IS NULL OR b.id <> :excludeId) thay vì ...AndIdNot: IdNot(null) sinh "id <> NULL"
    // (luôn sai trong SQL) nên khi tạo bài mới phép kiểm trùng sẽ không bao giờ khớp.

    @Query("SELECT COUNT(b) > 0 FROM Blog b WHERE b.status = com.vsl.social.entity.BlogStatus.PUBLISHED "
            + "AND b.normalizedTitle = :title AND (:excludeId IS NULL OR b.id <> :excludeId)")
    boolean existsPublishedWithTitle(@Param("title") String normalizedTitle, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(b) > 0 FROM Blog b WHERE b.status = com.vsl.social.entity.BlogStatus.PUBLISHED "
            + "AND b.normalizedTitle = :title AND b.contentFingerprint = :fingerprint "
            + "AND (:excludeId IS NULL OR b.id <> :excludeId)")
    boolean existsPublishedWithContent(@Param("title") String normalizedTitle, @Param("fingerprint") String fingerprint,
                                       @Param("excludeId") Long excludeId);

    /** Có bài nào TỪNG bị tố cáo (bất kể trạng thái) trùng tiêu đề + nội dung không. */
    @Query("SELECT COUNT(b) > 0 FROM Blog b WHERE b.normalizedTitle = :title AND b.contentFingerprint = :fingerprint "
            + "AND (:excludeId IS NULL OR b.id <> :excludeId) "
            + "AND EXISTS (SELECT 1 FROM BlogReport r WHERE r.blog = b)")
    boolean existsReportedWithContent(@Param("title") String normalizedTitle, @Param("fingerprint") String fingerprint,
                                      @Param("excludeId") Long excludeId);

    /** Ứng viên so trùng ngữ nghĩa bằng AI: bài công khai hoặc từng bị tố cáo, mới nhất trước. */
    @Query("SELECT b FROM Blog b WHERE (b.status = com.vsl.social.entity.BlogStatus.PUBLISHED "
            + "OR EXISTS (SELECT 1 FROM BlogReport r WHERE r.blog = b)) "
            + "AND (:excludeId IS NULL OR b.id <> :excludeId) ORDER BY b.createdAt DESC")
    List<Blog> findDuplicateCandidates(@Param("excludeId") Long excludeId, Pageable pageable);

    /** Bài cũ chưa có dạng chuẩn hóa (tạo trước khi thêm cột) - để bổ sung khi khởi động. */
    List<Blog> findByContentFingerprintIsNull();

    // ---- Danh sách ----

    /** Bảng tin: bài công khai, ưu tiên bài của người mình đang theo dõi, rồi mới nhất trước (như monolith). */
    @Query(value = "SELECT b FROM Blog b LEFT JOIN UserFollow f ON f.followeeId = b.authorId AND f.followerId = :viewerId "
            + "WHERE b.status = com.vsl.social.entity.BlogStatus.PUBLISHED "
            + "ORDER BY CASE WHEN f.id IS NULL THEN 1 ELSE 0 END, b.createdAt DESC",
            countQuery = "SELECT COUNT(b) FROM Blog b WHERE b.status = com.vsl.social.entity.BlogStatus.PUBLISHED")
    Page<Blog> findPublishedPrioritizingFollowed(@Param("viewerId") Long viewerId, Pageable pageable);

    /** Tìm bài công khai theo tiêu đề / nội dung / tác giả (authorIds lấy từ identity theo tên). */
    @Query("SELECT b FROM Blog b WHERE b.status = com.vsl.social.entity.BlogStatus.PUBLISHED AND ("
            + "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR b.authorId IN :authorIds)")
    Page<Blog> searchPublished(@Param("keyword") String keyword, @Param("authorIds") Collection<Long> authorIds,
                               Pageable pageable);

    /**
     * Danh sách cho admin: mọi bài KHÔNG phải nháp + nháp của chính admin (không xem nháp người khác).
     * status / keyword: lọc tùy chọn.
     */
    @Query("SELECT b FROM Blog b WHERE (b.status <> com.vsl.social.entity.BlogStatus.DRAFT OR b.authorId = :adminId) "
            + "AND (:status IS NULL OR b.status = :status) "
            + "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Blog> findForAdmin(@Param("adminId") Long adminId, @Param("status") BlogStatus status,
                            @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.likeCount), 0) FROM Blog b")
    Long sumLikeCount();

    @Query("SELECT COALESCE(SUM(b.commentCount), 0) FROM Blog b")
    Long sumCommentCount();

    @Query("SELECT COALESCE(SUM(b.shareCount), 0) FROM Blog b")
    Long sumShareCount();

    @Query("SELECT b FROM Blog b WHERE b.status = com.vsl.social.entity.BlogStatus.PUBLISHED ORDER BY (b.likeCount + b.commentCount + b.shareCount) DESC")
    java.util.List<Blog> findTopEngagedBlogs(Pageable pageable);

    @Query("SELECT b.authorId AS authorId, COUNT(b) AS totalPosts, " +
           "SUM(CASE WHEN b.status = com.vsl.social.entity.BlogStatus.PUBLISHED THEN 1 ELSE 0 END) AS publishedPosts, " +
           "COALESCE(SUM(b.likeCount), 0) AS totalLikesReceived " +
           "FROM Blog b GROUP BY b.authorId ORDER BY COUNT(b) DESC")
    java.util.List<com.vsl.social.repository.projection.CreatorStatProjection> findTopCreators(Pageable pageable);
}
