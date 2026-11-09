package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SdGoalItemDayResponse {
    private LocalDate date;
    private int plannedCount;
    private int actualCount;
    private boolean isWeekend;
    /** Per-topic breakdown of the same day's planned/actual counts, so the day-detail view
     * can show exactly which topic(s) fell short instead of only a track-wide total. */
    private List<SdGoalTopicDayBreakdown> topics;
}
