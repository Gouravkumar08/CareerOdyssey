package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DsaGoalItemResponse {
    private Long id;
    private Long sheetId;
    private String sheetName;
    private LocalDate startDate;
    private LocalDate endDate;
    /** One entry per topic in this goal, each independently computed. */
    private List<DsaGoalTopicTargetResponse> topics;
    /** True when every topic's target is done. */
    private boolean done;
    /** Convenience sum across topics. */
    private int overallRemainingCount;
    /** Convenience sum across topics. */
    private int overallTodayTargetCount;
}
