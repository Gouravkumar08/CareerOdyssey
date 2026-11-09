package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.SdTargetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SdTargetPlanRepository extends JpaRepository<SdTargetPlan, Long> {
    Optional<SdTargetPlan> findByUserId(Long userId);
}
