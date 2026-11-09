package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Independent daily target/progress for one topic within a {@link SdGoalItemResponse}.
 * System Design goals treat every topic in the goal as "complete every item under this
 * topic" — computed and reported separately per topic, never pooled across topics.
 */
@Getter
@Builder
public class SdGoalTopicStatus {
    private Long topicId;
    private String topicName;
    private int totalTarget;
    private int completedCount;
    private int remainingCount;
    private int todayTargetCount;
    private int todayCompletedCount;
    private boolean isWeekend;
    private boolean overdue;
    private Boolean yesterdayMet;
    private boolean done;
}
