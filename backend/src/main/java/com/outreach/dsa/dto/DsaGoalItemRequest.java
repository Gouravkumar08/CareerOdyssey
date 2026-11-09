package com.outreach.dsa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DsaGoalItemRequest {

    @NotEmpty
    @Valid
    private List<DsaGoalTopicTargetRequest> topics;

    /** Optional scope: null = across all sheets, else restrict to this sheet's questions. Shared across all topics. */
    private Long sheetId;

    @NotNull
    private LocalDate endDate;
}
