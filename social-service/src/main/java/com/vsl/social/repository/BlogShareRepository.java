package com.vsl.social.repository;

import com.vsl.social.entity.BlogShare;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogShareRepository extends JpaRepository<BlogShare, Long> {
}
