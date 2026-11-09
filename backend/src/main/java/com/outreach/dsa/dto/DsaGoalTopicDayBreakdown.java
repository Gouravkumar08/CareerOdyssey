package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DsaGoalTopicDayBreakdown {
    private Long topicId;
    private String topicName;
    private int plannedCount;
    private int actualCount;
}
