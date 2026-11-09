package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SdSheetStatsResponse {
    private long total;
    private long completed;
    private int overallPercent;
    private long topicsCount;
    private long importantTotal;
    private long importantSolved;
    private long hldTotal;
    private long hldCompleted;
    private long lldTotal;
    private long lldCompleted;
}
