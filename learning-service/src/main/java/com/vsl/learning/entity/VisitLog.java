package com.vsl.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Một lượt truy cập hệ thống. userId null = khách (guest) chưa đăng nhập.
 * Ghi 1 lần mỗi phiên trình duyệt (frontend gửi beacon khi mở app). Bê từ monolith,
 * đổi @ManyToOne User -> Long userId theo quy ước microservice.
 */
@Entity
@Table(name = "visit_logs", indexes = {
        @Index(name = "idx_visit_visited_at", columnList = "visited_at"),
        @Index(name = "idx_visit_session_id", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null = khách vãng lai (guest). */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Định danh 1 phiên tab trình duyệt (FE sinh, lưu sessionStorage). Dùng để "nâng cấp"
     * lượt của phiên từ GUEST -> USER khi khách đăng nhập, thay vì ghi thêm dòng mới.
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /** Mô tả thiết bị phân tích từ User-Agent, vd "Windows · Chrome". */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Vị trí suy ra từ IP (best-effort), vd "Ho Chi Minh, Vietnam". */
    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;
}
