package com.outreach.planner.dto;

import java.time.LocalDate;
import java.util.List;

public record PlannerCalendarDayResponse(LocalDate date, List<PlannerOccurrenceResponse> tasks) {
}
