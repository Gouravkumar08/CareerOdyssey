package com.outreach.planner.repository;

import com.outreach.planner.model.PlannerNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlannerNoteRepository extends JpaRepository<PlannerNote, Long> {
    List<PlannerNote> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<PlannerNote> findByIdAndUserId(Long id, Long userId);
}
