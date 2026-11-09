package com.outreach.dsa.dto;

import com.outreach.dsa.model.Difficulty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DsaGoalTopicTargetResponse {
    private Long topicId;
    private String topicName;
    /** null when this topic is in "complete all" mode. */
    private Integer targetQuestionCount;
    private List<Difficulty> difficulties;
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
