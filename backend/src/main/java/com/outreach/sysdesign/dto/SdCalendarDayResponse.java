package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SdCalendarDayResponse {
    private LocalDate date;
    private int completedCount;
    private int topicsCount;
    private List<SdItemSummary> items;
}
