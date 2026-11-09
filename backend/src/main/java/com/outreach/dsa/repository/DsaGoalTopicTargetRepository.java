package com.outreach.dsa.repository;

import com.outreach.dsa.model.DsaGoalTopicTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DsaGoalTopicTargetRepository extends JpaRepository<DsaGoalTopicTarget, Long> {
    List<DsaGoalTopicTarget> findByGoalItemId(Long goalItemId);

    void deleteByGoalItemId(Long goalItemId);
}
