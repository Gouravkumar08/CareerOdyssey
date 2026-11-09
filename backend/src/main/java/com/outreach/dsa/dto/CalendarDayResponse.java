package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CalendarDayResponse {
    private LocalDate date;
    private int solvedCount;
    private List<QuestionSummary> questions;
}
