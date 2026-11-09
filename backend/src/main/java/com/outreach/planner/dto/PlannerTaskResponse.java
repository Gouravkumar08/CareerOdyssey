package com.outreach.planner.dto;

import com.outreach.planner.model.PlannerTask;
import com.outreach.planner.model.TaskPriority;
import com.outreach.planner.model.TaskRecurrence;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record PlannerTaskResponse(
        Long id,
        String title,
        String description,
        TaskPriority priority,
        TaskRecurrence recurrence,
        Set<DayOfWeek> weekdays,
        LocalDate startDate,
        LocalDate endDate,
        String progressUnit,
        Integer progressCurrent,
        Integer progressTarget,
        Instant updatedAt) {

    public static PlannerTaskResponse from(PlannerTask t) {
        return new PlannerTaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getRecurrence(),
                t.getWeekdays(), t.getStartDate(), t.getEndDate(), t.getProgressUnit(), t.getProgressCurrent(),
                t.getProgressTarget(), t.getUpdatedAt());
    }
}
