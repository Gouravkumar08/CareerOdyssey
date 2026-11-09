package com.outreach.dsa.repository;

import com.outreach.dsa.model.TargetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TargetPlanRepository extends JpaRepository<TargetPlan, Long> {
    Optional<TargetPlan> findByUserId(Long userId);
}
