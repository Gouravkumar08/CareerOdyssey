package com.outreach.planner.dto;

import com.outreach.planner.model.PlannerNote;

import java.time.Instant;

public record PlannerNoteResponse(Long id, String content, Instant updatedAt) {

    public static PlannerNoteResponse from(PlannerNote n) {
        return new PlannerNoteResponse(n.getId(), n.getContent(), n.getUpdatedAt());
    }
}
