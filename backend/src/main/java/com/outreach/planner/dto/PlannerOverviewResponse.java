package com.outreach.planner.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the dashboard and the planner page need to answer "what's on today?" in one
 * request. {@code progress} lists unfinished progress-tracking tasks (e.g. books in progress).
 */
public record PlannerOverviewResponse(
        LocalDate date,
        List<PlannerOccurrenceResponse> today,
        List<PlannerOccurrenceResponse> overdue,
        List<PlannerOccurrenceResponse> upcoming,
        List<PlannerTaskResponse> progress,
        List<PlannerNoteResponse> notes) {
}
