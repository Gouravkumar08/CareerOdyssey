package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SdGoalTopicDayBreakdown {
    private Long topicId;
    private String topicName;
    private int plannedCount;
    private int actualCount;
}
