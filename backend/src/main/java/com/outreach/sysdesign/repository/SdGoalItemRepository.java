package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.SdGoalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SdGoalItemRepository extends JpaRepository<SdGoalItem, Long> {
    List<SdGoalItem> findByUserId(Long userId);

    Optional<SdGoalItem> findByIdAndUserId(Long id, Long userId);
}
