package com.vsl.identity.repository;

import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    long countByStatus(UserStatus status);

    long countByRole(Role role);
}
