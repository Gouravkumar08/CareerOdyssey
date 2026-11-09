package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SdGoalItemResponse {
    private Long id;
    private Long sheetId;
    private String sheetName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<SdGoalTopicStatus> topics;
    private boolean done;
    private int overallRemainingCount;
    private int overallTodayTargetCount;
}
