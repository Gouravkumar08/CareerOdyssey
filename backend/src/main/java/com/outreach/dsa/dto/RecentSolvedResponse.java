package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RecentSolvedResponse {
    private Long questionId;
    private String title;
    private String topicName;
    private Instant solvedAt;
}
