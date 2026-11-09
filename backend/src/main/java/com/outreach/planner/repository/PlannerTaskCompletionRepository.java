package com.outreach.planner.repository;

import com.outreach.planner.model.PlannerTaskCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PlannerTaskCompletionRepository extends JpaRepository<PlannerTaskCompletion, Long> {
    List<PlannerTaskCompletion> findByTaskIdInAndOccurrenceDateBetween(Collection<Long> taskIds, LocalDate from, LocalDate to);

    boolean existsByTaskIdAndOccurrenceDate(Long taskId, LocalDate occurrenceDate);

    void deleteByTaskIdAndOccurrenceDate(Long taskId, LocalDate occurrenceDate);

    void deleteByTaskId(Long taskId);
}
