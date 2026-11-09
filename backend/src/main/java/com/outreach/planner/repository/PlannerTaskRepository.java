package com.outreach.planner.repository;

import com.outreach.planner.model.PlannerTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlannerTaskRepository extends JpaRepository<PlannerTask, Long> {
    List<PlannerTask> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PlannerTask> findByIdAndUserId(Long id, Long userId);
}
