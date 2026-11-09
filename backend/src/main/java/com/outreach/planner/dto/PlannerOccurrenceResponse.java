package com.outreach.planner.dto;

import com.outreach.planner.model.TaskPriority;
import com.outreach.planner.model.TaskRecurrence;

import java.time.LocalDate;

/** One task on one date, derived from the task's recurrence rule. */
public record PlannerOccurrenceResponse(
        Long taskId,
        String title,
        LocalDate date,
        TaskPriority priority,
        TaskRecurrence recurrence,
        boolean completed) {
}
