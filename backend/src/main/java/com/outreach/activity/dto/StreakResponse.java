package com.outreach.activity.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StreakResponse {
    private int currentStreak;
    private String message;
}
