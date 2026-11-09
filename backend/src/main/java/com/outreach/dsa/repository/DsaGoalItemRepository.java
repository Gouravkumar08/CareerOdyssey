package com.outreach.dsa.repository;

import com.outreach.dsa.model.DsaGoalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DsaGoalItemRepository extends JpaRepository<DsaGoalItem, Long> {
    List<DsaGoalItem> findByUserId(Long userId);

    Optional<DsaGoalItem> findByIdAndUserId(Long id, Long userId);
}
