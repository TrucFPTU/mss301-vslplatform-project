package com.vsl.learning.repository;

import com.vsl.learning.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    /** Từ yêu thích của user kèm từ vựng + chủ đề, mới đánh dấu trước. */
    @Query("SELECT f FROM UserFavorite f JOIN FETCH f.vocabulary v LEFT JOIN FETCH v.category "
            + "WHERE f.userId = :userId ORDER BY f.createdAt DESC, f.id DESC")
    List<UserFavorite> findAllWithVocabularyByUserId(@Param("userId") Long userId);

    /** Thêm yêu thích; đã có (unique user_id + vocabulary_id) thì bỏ qua — an toàn khi bấm 2 lần / gọi đồng thời. */
    @Modifying
    @Query(value = "INSERT INTO user_favorites (user_id, vocabulary_id, created_at) "
            + "VALUES (:userId, :vocabularyId, :createdAt) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("vocabularyId") Long vocabularyId,
                        @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Query("DELETE FROM UserFavorite f WHERE f.userId = :userId AND f.vocabulary.id = :vocabularyId")
    int deleteByUserIdAndVocabularyId(@Param("userId") Long userId, @Param("vocabularyId") Long vocabularyId);

    @Modifying
    @Query("DELETE FROM UserFavorite f WHERE f.vocabulary.id = :vocabularyId")
    int deleteByVocabularyId(@Param("vocabularyId") Long vocabularyId);
}
