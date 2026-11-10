package com.vsl.identity.repository;

import com.vsl.identity.entity.OutboxEvent;
import com.vsl.identity.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Sự kiện có trạng thái cho trước đã tới hạn gửi, cũ trước.
     * Relay chỉ chạy 1 instance nên không khóa dòng; nếu scale nhiều instance thì đổi sang native
     * {@code SELECT ... FOR UPDATE SKIP LOCKED}.
     */
    @Query("select e from OutboxEvent e where e.status = :status and e.nextAttemptAt <= :now order by e.id")
    List<OutboxEvent> findDue(@Param("status") OutboxStatus status, @Param("now") LocalDateTime now, Pageable page);

    boolean existsByEventTypeAndAggregateIdAndDestination(String eventType, Long aggregateId, String destination);
}
