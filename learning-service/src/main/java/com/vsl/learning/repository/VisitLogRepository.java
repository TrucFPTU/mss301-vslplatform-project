package com.vsl.learning.repository;

import com.vsl.learning.entity.VisitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    /** Danh sách log mới nhất trước - cho bảng chi tiết phân trang. */
    Page<VisitLog> findAllByOrderByVisitedAtDesc(Pageable pageable);

    /** Tìm lượt truy cập theo sessionId để upsert (nâng cấp guest -> user). */
    Optional<VisitLog> findFirstBySessionId(String sessionId);

    /** Chỉ lấy mốc thời gian trong khoảng - để gom nhóm cho biểu đồ. */
    @Query("SELECT v.visitedAt FROM VisitLog v WHERE v.visitedAt >= :from AND v.visitedAt < :to")
    List<LocalDateTime> findVisitedAtBetween(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}
