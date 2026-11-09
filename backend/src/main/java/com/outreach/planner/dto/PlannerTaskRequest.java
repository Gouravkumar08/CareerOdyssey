package com.outreach.planner.dto;

import com.outreach.planner.model.TaskPriority;
import com.outreach.planner.model.TaskRecurrence;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record PlannerTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 1000) String description,
        TaskPriority priority,
        @NotNull TaskRecurrence recurrence,
        Set<DayOfWeek> weekdays,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Size(max = 30) String progressUnit,
        @Min(0) Integer progressCurrent,
        @Positive Integer progressTarget) {
}
