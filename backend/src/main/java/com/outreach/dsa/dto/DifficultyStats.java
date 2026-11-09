package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DifficultyStats {
    private long total;
    private long solved;
}
