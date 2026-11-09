package com.outreach.planner.service;

import com.outreach.planner.dto.PlannerCalendarDayResponse;
import com.outreach.planner.dto.PlannerOccurrenceResponse;
import com.outreach.planner.dto.PlannerOverviewResponse;
import com.outreach.planner.dto.PlannerProgressRequest;
import com.outreach.planner.dto.PlannerTaskRequest;
import com.outreach.planner.dto.PlannerTaskResponse;
import com.outreach.planner.model.PlannerTask;
import com.outreach.planner.model.PlannerTaskCompletion;
import com.outreach.planner.model.TaskPriority;
import com.outreach.planner.model.TaskRecurrence;
import com.outreach.planner.repository.PlannerTaskCompletionRepository;
import com.outreach.planner.repository.PlannerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenericPlannerService {

    /** How far ahead "upcoming" looks, in days after today; the client decides how much of it to show. */
    static final int UPCOMING_DAYS = 2;
    /**
     * How far back a missed occurrence of a recurring task still counts as overdue. A one-time task
     * has a single deadline, so it stays overdue until completed.
     */
    static final int OVERDUE_LOOKBACK_DAYS = 7;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final Comparator<PlannerOccurrenceResponse> BY_DATE_THEN_PRIORITY = Comparator
            .comparing(PlannerOccurrenceResponse::date)
            .thenComparing(PlannerOccurrenceResponse::priority, Comparator.reverseOrder())
            .thenComparing(PlannerOccurrenceResponse::title, String.CASE_INSENSITIVE_ORDER);

    private record OccurrenceKey(Long taskId, LocalDate date) {}

    private final PlannerTaskRepository taskRepository;
    private final PlannerTaskCompletionRepository completionRepository;
    private final PlannerNoteService noteService;

    @Transactional(readOnly = true)
    public List<PlannerTaskResponse> listTasks(Long userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(PlannerTaskResponse::from).toList();
    }

    @Transactional
    public PlannerTaskResponse createTask(Long userId, PlannerTaskRequest request) {
        PlannerTask task = PlannerTask.builder().userId(userId).build();
        apply(task, request);
        return PlannerTaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public PlannerTaskResponse updateTask(Long userId, Long taskId, PlannerTaskRequest request) {
        PlannerTask task = findTask(userId, taskId);
        apply(task, request);
        return PlannerTaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        PlannerTask task = findTask(userId, taskId);
        completionRepository.deleteByTaskId(task.getId());
        taskRepository.delete(task);
    }

    @Transactional
    public void complete(Long userId, Long taskId, LocalDate date) {
        PlannerTask task = findTask(userId, taskId);
        if (!task.occursOn(date)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This task isn't scheduled on " + date);
        }
        if (!completionRepository.existsByTaskIdAndOccurrenceDate(taskId, date)) {
            completionRepository.save(PlannerTaskCompletion.builder().taskId(taskId).occurrenceDate(date).build());
        }
    }

    @Transactional
    public void uncomplete(Long userId, Long taskId, LocalDate date) {
        findTask(userId, taskId);
        completionRepository.deleteByTaskIdAndOccurrenceDate(taskId, date);
    }

    @Transactional
    public PlannerTaskResponse updateProgress(Long userId, Long taskId, PlannerProgressRequest request) {
        PlannerTask task = findTask(userId, taskId);
        if (task.getProgressTarget() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This task doesn't track progress");
        }
        if (request.current() > task.getProgressTarget()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress can't exceed the target of " + task.getProgressTarget());
        }
        task.setProgressCurrent(request.current());
        return PlannerTaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public PlannerOverviewResponse overview(Long userId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate until = today.plusDays(UPCOMING_DAYS);
        LocalDate recentFrom = today.minusDays(OVERDUE_LOOKBACK_DAYS);
        List<PlannerTask> tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // One-time tasks are looked at from their own due date; recurring ones from the lookback window.
        Map<PlannerTask, LocalDate> scanFrom = tasks.stream().collect(Collectors.toMap(t -> t,
                t -> t.getRecurrence() == TaskRecurrence.NONE && t.getStartDate().isBefore(recentFrom) ? t.getStartDate() : recentFrom));
        LocalDate earliest = scanFrom.values().stream().min(LocalDate::compareTo).orElse(recentFrom);
        Set<OccurrenceKey> done = completedKeys(tasks, earliest, until);

        List<PlannerOccurrenceResponse> todayList = new ArrayList<>();
        List<PlannerOccurrenceResponse> overdue = new ArrayList<>();
        List<PlannerOccurrenceResponse> upcoming = new ArrayList<>();
        for (PlannerTask task : tasks) {
            for (LocalDate date : occurrences(task, scanFrom.get(task), until)) {
                PlannerOccurrenceResponse occurrence = toOccurrence(task, date, done.contains(new OccurrenceKey(task.getId(), date)));
                if (date.isAfter(today)) {
                    upcoming.add(occurrence);
                } else if (date.equals(today)) {
                    todayList.add(occurrence);
                } else if (!occurrence.completed()) {
                    overdue.add(occurrence);
                }
            }
        }

        List<PlannerTaskResponse> progress = tasks.stream()
                .filter(t -> t.getProgressTarget() != null && t.getProgressCurrent() < t.getProgressTarget())
                .map(PlannerTaskResponse::from)
                .toList();

        return new PlannerOverviewResponse(today,
                todayList.stream().sorted(BY_DATE_THEN_PRIORITY).toList(),
                overdue.stream().sorted(BY_DATE_THEN_PRIORITY).toList(),
                upcoming.stream().sorted(BY_DATE_THEN_PRIORITY).toList(),
                progress,
                noteService.list(userId));
    }

    @Transactional(readOnly = true)
    public List<PlannerCalendarDayResponse> calendar(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<PlannerTask> tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Set<OccurrenceKey> done = completedKeys(tasks, from, to);

        Map<LocalDate, List<PlannerOccurrenceResponse>> byDate = new TreeMap<>();
        for (PlannerTask task : tasks) {
            for (LocalDate date : occurrences(task, from, to)) {
                byDate.computeIfAbsent(date, d -> new ArrayList<>())
                        .add(toOccurrence(task, date, done.contains(new OccurrenceKey(task.getId(), date))));
            }
        }
        return byDate.entrySet().stream()
                .map(e -> new PlannerCalendarDayResponse(e.getKey(), e.getValue().stream().sorted(BY_DATE_THEN_PRIORITY).toList()))
                .toList();
    }

    private PlannerTask findTask(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task " + taskId + " not found"));
    }

    private Set<OccurrenceKey> completedKeys(List<PlannerTask> tasks, LocalDate from, LocalDate to) {
        if (tasks.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = tasks.stream().map(PlannerTask::getId).toList();
        return completionRepository.findByTaskIdInAndOccurrenceDateBetween(ids, from, to).stream()
                .map(c -> new OccurrenceKey(c.getTaskId(), c.getOccurrenceDate()))
                .collect(Collectors.toSet());
    }

    /** Dates in [from, to] on which the task is scheduled. */
    private List<LocalDate> occurrences(PlannerTask task, LocalDate from, LocalDate to) {
        if (task.getRecurrence() == TaskRecurrence.NONE) {
            LocalDate due = task.getStartDate();
            return !due.isBefore(from) && !due.isAfter(to) ? List.of(due) : List.of();
        }
        List<LocalDate> dates = new ArrayList<>();
        LocalDate last = task.getEndDate() != null && task.getEndDate().isBefore(to) ? task.getEndDate() : to;
        for (LocalDate d = from.isBefore(task.getStartDate()) ? task.getStartDate() : from; !d.isAfter(last); d = d.plusDays(1)) {
            if (task.occursOn(d)) {
                dates.add(d);
            }
        }
        return dates;
    }

    private PlannerOccurrenceResponse toOccurrence(PlannerTask task, LocalDate date, boolean completed) {
        return new PlannerOccurrenceResponse(task.getId(), task.getTitle(), date, task.getPriority(), task.getRecurrence(), completed);
    }

    private void apply(PlannerTask task, PlannerTaskRequest request) {
        TaskRecurrence recurrence = request.recurrence();
        Set<DayOfWeek> weekdays = request.weekdays() == null ? Set.<DayOfWeek>of() : request.weekdays();
        if (recurrence == TaskRecurrence.WEEKLY && weekdays.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick at least one weekday for a weekly task");
        }
        if (recurrence != TaskRecurrence.NONE && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date can't be before the start date");
        }
        Integer target = request.progressTarget();
        if (target != null && request.progressCurrent() != null && request.progressCurrent() > target) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress can't exceed the target of " + target);
        }

        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setRecurrence(recurrence);
        task.setWeekdays(recurrence == TaskRecurrence.WEEKLY ? EnumSet.copyOf(weekdays) : EnumSet.noneOf(DayOfWeek.class));
        task.setStartDate(request.startDate());
        task.setEndDate(recurrence == TaskRecurrence.NONE ? null : request.endDate());
        task.setProgressTarget(target);
        task.setProgressUnit(target == null ? null : blankToNull(request.progressUnit()));
        task.setProgressCurrent(target == null ? null : request.progressCurrent() == null ? Integer.valueOf(0) : request.progressCurrent());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
