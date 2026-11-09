package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TargetPlanResponse {
    private int dailyTargetCount;
    private Long sheetId;
    private LocalDate startDate;
    private int todaySolvedCount;
}
