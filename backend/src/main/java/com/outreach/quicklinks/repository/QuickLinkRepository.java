package com.outreach.quicklinks.repository;

import com.outreach.quicklinks.model.QuickLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuickLinkRepository extends JpaRepository<QuickLink, Long> {
    List<QuickLink> findByUserIdOrderByOrderIndexAscIdAsc(Long userId);

    Optional<QuickLink> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
