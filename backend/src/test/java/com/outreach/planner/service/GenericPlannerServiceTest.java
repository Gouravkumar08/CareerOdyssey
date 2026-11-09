package com.outreach.planner.service;

import com.outreach.planner.dto.PlannerOverviewResponse;
import com.outreach.planner.dto.PlannerProgressRequest;
import com.outreach.planner.dto.PlannerTaskRequest;
import com.outreach.planner.model.PlannerTask;
import com.outreach.planner.model.PlannerTaskCompletion;
import com.outreach.planner.model.TaskPriority;
import com.outreach.planner.model.TaskRecurrence;
import com.outreach.planner.repository.PlannerTaskCompletionRepository;
import com.outreach.planner.repository.PlannerTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericPlannerServiceTest {

    private static final Long USER = 1L;

    @Mock PlannerTaskRepository taskRepository;
    @Mock PlannerTaskCompletionRepository completionRepository;
    @Mock PlannerNoteService noteService;

    GenericPlannerService service;
    LocalDate today;

    @BeforeEach
    void setUp() {
        service = new GenericPlannerService(taskRepository, completionRepository, noteService);
        today = LocalDate.now();
        lenient().when(noteService.list(USER)).thenReturn(List.of());
        lenient().when(completionRepository.findByTaskIdInAndOccurrenceDateBetween(anyCollection(), any(), any()))
                .thenReturn(List.of());
    }

    private PlannerTask task(long id, String title, TaskRecurrence recurrence, LocalDate start, LocalDate end) {
        return PlannerTask.builder().id(id).userId(USER).title(title).priority(TaskPriority.MEDIUM)
                .recurrence(recurrence).startDate(start).endDate(end).build();
    }

    private void givenTasks(PlannerTask... tasks) {
        when(taskRepository.findByUserIdOrderByCreatedAtDesc(USER)).thenReturn(List.of(tasks));
    }

    @Test
    void dailyTaskShowsTodayAndNextTwoDays() {
        givenTasks(task(1, "Workout", TaskRecurrence.DAILY, today.minusDays(30), null));

        PlannerOverviewResponse overview = service.overview(USER);

        assertEquals(1, overview.today().size());
        assertEquals(GenericPlannerService.UPCOMING_DAYS, overview.upcoming().size());
        assertEquals(today.plusDays(1), overview.upcoming().get(0).date());
    }

    @Test
    void missedDailyOccurrencesAreOverdueOnlyWithinLookbackWindow() {
        givenTasks(task(1, "Workout", TaskRecurrence.DAILY, today.minusDays(60), null));

        PlannerOverviewResponse overview = service.overview(USER);

        assertEquals(GenericPlannerService.OVERDUE_LOOKBACK_DAYS, overview.overdue().size());
    }

    @Test
    void completedPastOccurrenceIsNotOverdue() {
        PlannerTask t = task(1, "Workout", TaskRecurrence.DAILY, today.minusDays(60), null);
        givenTasks(t);
        when(completionRepository.findByTaskIdInAndOccurrenceDateBetween(anyCollection(), any(), any()))
                .thenReturn(List.of(PlannerTaskCompletion.builder().taskId(1L).occurrenceDate(today.minusDays(1)).build()));

        PlannerOverviewResponse overview = service.overview(USER);

        assertEquals(GenericPlannerService.OVERDUE_LOOKBACK_DAYS - 1, overview.overdue().size());
        assertTrue(overview.overdue().stream().noneMatch(o -> o.date().equals(today.minusDays(1))));
    }

    @Test
    void oneTimeTaskStaysOverdueBeyondLookbackUntilCompleted() {
        givenTasks(task(1, "Finish resume", TaskRecurrence.NONE, today.minusDays(40), null));

        PlannerOverviewResponse overview = service.overview(USER);

        assertEquals(1, overview.overdue().size());
        assertEquals(today.minusDays(40), overview.overdue().get(0).date());
    }

    @Test
    void completedTodayIsFlaggedAndStaysInTodayList() {
        givenTasks(task(1, "Review finances", TaskRecurrence.NONE, today, null));
        when(completionRepository.findByTaskIdInAndOccurrenceDateBetween(anyCollection(), any(), any()))
                .thenReturn(List.of(PlannerTaskCompletion.builder().taskId(1L).occurrenceDate(today).build()));

        PlannerOverviewResponse overview = service.overview(USER);

        assertEquals(1, overview.today().size());
        assertTrue(overview.today().get(0).completed());
        assertTrue(overview.overdue().isEmpty());
    }

    @Test
    void progressListContainsOnlyUnfinishedProgressTasks() {
        PlannerTask reading = task(1, "Atomic Habits", TaskRecurrence.DAILY, today, null);
        reading.setProgressTarget(320);
        reading.setProgressCurrent(127);
        PlannerTask finished = task(2, "Old book", TaskRecurrence.DAILY, today, null);
        finished.setProgressTarget(100);
        finished.setProgressCurrent(100);
        givenTasks(reading, finished, task(3, "Workout", TaskRecurrence.DAILY, today, null));

        PlannerOverviewResponse overview = service.overview(USER);

        assertEquals(1, overview.progress().size());
        assertEquals("Atomic Habits", overview.progress().get(0).title());
    }

    @Test
    void completingAnUnscheduledDateIsRejected() {
        // a Monday-only task, asked to complete on a Tuesday
        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        PlannerTask t = task(1, "Gym", TaskRecurrence.WEEKLY, monday.minusWeeks(1), null);
        t.setWeekdays(EnumSet.of(DayOfWeek.MONDAY));
        when(taskRepository.findByIdAndUserId(1L, USER)).thenReturn(java.util.Optional.of(t));

        assertThrows(ResponseStatusException.class, () -> service.complete(USER, 1L, monday.plusDays(1)));
        verify(completionRepository, never()).save(any());
    }

    @Test
    void completingTwiceStoresOneCompletion() {
        PlannerTask t = task(1, "Workout", TaskRecurrence.DAILY, today.minusDays(1), null);
        when(taskRepository.findByIdAndUserId(1L, USER)).thenReturn(java.util.Optional.of(t));
        when(completionRepository.existsByTaskIdAndOccurrenceDate(1L, today)).thenReturn(true);

        service.complete(USER, 1L, today);

        verify(completionRepository, never()).save(any());
    }

    @Test
    void weeklyTaskWithoutWeekdaysIsRejected() {
        PlannerTaskRequest request = new PlannerTaskRequest("Gym", null, null, TaskRecurrence.WEEKLY, Set.of(),
                today, null, null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.createTask(USER, request));
    }

    @Test
    void endBeforeStartIsRejected() {
        PlannerTaskRequest request = new PlannerTaskRequest("Read", null, null, TaskRecurrence.DAILY, null,
                today, today.minusDays(1), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.createTask(USER, request));
    }

    @Test
    void progressCannotExceedTarget() {
        PlannerTask t = task(1, "Atomic Habits", TaskRecurrence.DAILY, today, null);
        t.setProgressTarget(320);
        t.setProgressCurrent(10);
        when(taskRepository.findByIdAndUserId(1L, USER)).thenReturn(java.util.Optional.of(t));

        assertThrows(ResponseStatusException.class, () -> service.updateProgress(USER, 1L, new PlannerProgressRequest(321)));
    }
}
