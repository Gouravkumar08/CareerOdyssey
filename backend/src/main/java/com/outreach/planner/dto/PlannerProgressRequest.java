package com.outreach.planner.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlannerProgressRequest(@NotNull @Min(0) Integer current) {
}
