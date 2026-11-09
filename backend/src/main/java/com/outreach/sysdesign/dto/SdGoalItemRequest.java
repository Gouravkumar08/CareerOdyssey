package com.outreach.sysdesign.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SdGoalItemRequest {

    @NotEmpty
    private List<Long> topicIds;

    private Long sheetId;

    @NotNull
    private LocalDate endDate;
}
