package com.outreach.dsa.dto;

import com.outreach.dsa.model.Difficulty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DsaGoalTopicTargetRequest {

    @NotNull
    private Long topicId;

    /** null = "complete every matching question in this topic"; set (>= 1) = "solve this many". */
    private Integer targetQuestionCount;

    /** null/empty = no restriction (all difficulties count). */
    private List<Difficulty> difficulties;
}
