package com.outreach.sysdesign.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdTargetPlanRequest {

    @NotNull
    @Min(0)
    private Integer dailyTargetCount;

    private Long sheetId;
}
