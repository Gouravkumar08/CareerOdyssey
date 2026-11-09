package com.outreach.planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlannerNoteRequest(@NotBlank @Size(max = 2000) String content) {
}
