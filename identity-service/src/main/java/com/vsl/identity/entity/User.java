package com.vsl.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    // DEFAULT 'ACTIVE': DB dev có thể đã có bảng users theo model cũ (is_active) -> thêm cột không lỗi.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'ACTIVE'")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /** Mặc định TẮT — người dùng phải tự BẬT để nhận email nhắc nhở streak. */
    // columnDefinition có DEFAULT để ddl-auto=update thêm được cột vào bảng users đã có dữ liệu.
    @Column(name = "email_notifications_enabled", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean emailNotificationsEnabled = false;

    /**
     * Ngày đã gửi mail nhắc streak lần cuối. Set null khi người dùng luyện tập lại
     * (do streak-scheduler quét theo ngày) — tránh gửi trùng trong cùng một ngày.
     */
    @Column(name = "streak_reminder_sent_date")
    private java.time.LocalDate streakReminderSentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Xóa mềm + ẩn danh hóa: giữ userId (bài viết / lịch sử ở service khác vẫn tham chiếu được) nhưng bỏ
     * thông tin cá nhân; đổi email/username để người đó có thể đăng ký lại.
     */
    public void anonymize() {
        this.status = UserStatus.DELETED;
        this.fullName = "Người dùng đã xóa";
        this.username = "deleted_" + userId;
        this.email = "deleted+" + userId + "@deleted.local";
        this.avatarUrl = null;
        this.bio = null;
        this.emailNotificationsEnabled = false;
    }

    /** Tài khoản còn hoạt động = trạng thái ACTIVE. Nguồn sự thật là {@link #status}. */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (role == null) {
            role = Role.USER;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
