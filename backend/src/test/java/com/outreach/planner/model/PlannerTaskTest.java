package com.outreach.planner.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerTaskTest {

    private static PlannerTask task(TaskRecurrence recurrence, LocalDate start, LocalDate end, DayOfWeek... weekdays) {
        return PlannerTask.builder()
                .recurrence(recurrence)
                .startDate(start)
                .endDate(end)
                .weekdays(weekdays.length == 0 ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.of(weekdays[0], weekdays))
                .build();
    }

    @Test
    void oneTimeOccursOnlyOnItsDueDate() {
        PlannerTask t = task(TaskRecurrence.NONE, LocalDate.of(2026, 10, 5), null);
        assertTrue(t.occursOn(LocalDate.of(2026, 10, 5)));
        assertFalse(t.occursOn(LocalDate.of(2026, 10, 4)));
        assertFalse(t.occursOn(LocalDate.of(2026, 10, 6)));
    }

    @Test
    void dailyRespectsStartAndEnd() {
        PlannerTask t = task(TaskRecurrence.DAILY, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30));
        assertFalse(t.occursOn(LocalDate.of(2026, 9, 30)));
        assertTrue(t.occursOn(LocalDate.of(2026, 10, 1)));
        assertTrue(t.occursOn(LocalDate.of(2026, 10, 30)));
        assertFalse(t.occursOn(LocalDate.of(2026, 10, 31)));
    }

    @Test
    void dailyWithoutEndRunsForever() {
        PlannerTask t = task(TaskRecurrence.DAILY, LocalDate.of(2026, 10, 1), null);
        assertTrue(t.occursOn(LocalDate.of(2030, 1, 1)));
    }

    @Test
    void weeklyOccursOnlyOnChosenWeekdays() {
        // 2026-10-05 is a Monday
        PlannerTask t = task(TaskRecurrence.WEEKLY, LocalDate.of(2026, 10, 1), null,
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertTrue(t.occursOn(LocalDate.of(2026, 10, 5)));
        assertFalse(t.occursOn(LocalDate.of(2026, 10, 6)));
        assertTrue(t.occursOn(LocalDate.of(2026, 10, 7)));
        assertTrue(t.occursOn(LocalDate.of(2026, 10, 9)));
        assertFalse(t.occursOn(LocalDate.of(2026, 10, 10)));
    }

    @Test
    void monthlyRepeatsOnStartDayOfMonth() {
        PlannerTask t = task(TaskRecurrence.MONTHLY, LocalDate.of(2026, 10, 1), null);
        assertTrue(t.occursOn(LocalDate.of(2026, 11, 1)));
        assertFalse(t.occursOn(LocalDate.of(2026, 11, 2)));
    }

    @Test
    void monthlyOnThe31stFallsBackToLastDayOfShorterMonths() {
        PlannerTask t = task(TaskRecurrence.MONTHLY, LocalDate.of(2026, 1, 31), null);
        assertTrue(t.occursOn(LocalDate.of(2026, 2, 28)));
        assertFalse(t.occursOn(LocalDate.of(2026, 2, 27)));
        assertTrue(t.occursOn(LocalDate.of(2026, 4, 30)));
        assertTrue(t.occursOn(LocalDate.of(2026, 5, 31)));
    }
}
