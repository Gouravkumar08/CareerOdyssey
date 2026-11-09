package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SheetStatsResponse {
    private long totalQuestions;
    private long solvedQuestions;
    private int overallPercent;
    private long topicsCount;
    private long importantTotal;
    private long importantSolved;
    private DifficultyStats easy;
    private DifficultyStats medium;
    private DifficultyStats hard;
}
