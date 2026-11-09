package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SdTargetPlanResponse {
    private int dailyTargetCount;
    private Long sheetId;
    private LocalDate startDate;
    private int todayCompletedCount;
}
