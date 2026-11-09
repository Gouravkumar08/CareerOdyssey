package com.outreach.dsa.service;

import com.outreach.dsa.dto.CalendarDayResponse;
import com.outreach.dsa.dto.DsaGoalItemDayResponse;
import com.outreach.dsa.dto.DsaGoalItemRequest;
import com.outreach.dsa.dto.DsaGoalItemResponse;
import com.outreach.dsa.dto.DsaGoalTopicDayBreakdown;
import com.outreach.dsa.dto.DsaGoalTopicTargetRequest;
import com.outreach.dsa.dto.DsaGoalTopicTargetResponse;
import com.outreach.dsa.dto.QuestionSummary;
import com.outreach.dsa.dto.RecentSolvedResponse;
import com.outreach.dsa.dto.TargetPlanRequest;
import com.outreach.dsa.dto.TargetPlanResponse;
import com.outreach.dsa.exception.ResourceNotFoundException;
import com.outreach.dsa.model.DsaGoalItem;
import com.outreach.dsa.model.DsaGoalTopicTarget;
import com.outreach.dsa.model.Question;
import com.outreach.dsa.model.Sheet;
import com.outreach.dsa.model.TargetPlan;
import com.outreach.dsa.model.Topic;
import com.outreach.dsa.model.UserQuestionProgress;
import com.outreach.dsa.repository.DsaGoalItemRepository;
import com.outreach.dsa.repository.DsaGoalTopicTargetRepository;
import com.outreach.dsa.repository.QuestionRepository;
import com.outreach.dsa.repository.SheetRepository;
import com.outreach.dsa.repository.TargetPlanRepository;
import com.outreach.dsa.repository.TopicRepository;
import com.outreach.dsa.repository.UserQuestionProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannerService {

    private final TargetPlanRepository targetPlanRepository;
    private final UserQuestionProgressRepository progressRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final SheetRepository sheetRepository;
    private final DsaGoalItemRepository dsaGoalItemRepository;
    private final DsaGoalTopicTargetRepository dsaGoalTopicTargetRepository;
    private final QuestionScopeResolver questionScopeResolver;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Transactional(readOnly = true)
    public TargetPlanResponse getTarget(Long userId) {
        TargetPlan plan = targetPlanRepository.findByUserId(userId).orElse(null);
        int todaySolved = countSolvedOn(userId, LocalDate.now(ZONE));
        if (plan == null) {
            return TargetPlanResponse.builder().dailyTargetCount(0).sheetId(null).startDate(null).todaySolvedCount(todaySolved).build();
        }
        return TargetPlanResponse.builder()
                .dailyTargetCount(plan.getDailyTargetCount())
                .sheetId(plan.getSheetId())
                .startDate(plan.getStartDate())
                .todaySolvedCount(todaySolved)
                .build();
    }

    @Transactional
    public TargetPlanResponse setTarget(Long userId, TargetPlanRequest request) {
        TargetPlan plan = targetPlanRepository.findByUserId(userId)
                .orElseGet(() -> TargetPlan.builder().userId(userId).startDate(LocalDate.now(ZONE)).build());
        plan.setDailyTargetCount(request.getDailyTargetCount());
        plan.setSheetId(request.getSheetId());
        TargetPlan saved = targetPlanRepository.save(plan);
        return TargetPlanResponse.builder()
                .dailyTargetCount(saved.getDailyTargetCount())
                .sheetId(saved.getSheetId())
                .startDate(saved.getStartDate())
                .todaySolvedCount(countSolvedOn(userId, LocalDate.now(ZONE)))
                .build();
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> calendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        Instant from = yearMonth.atDay(1).atStartOfDay(ZONE).toInstant();
        Instant to = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZONE).toInstant();

        List<UserQuestionProgress> solvedInRange = progressRepository.findByUserIdAndSolvedTrueAndSolvedAtBetween(userId, from, to);
        Map<Long, Question> questionsById = new HashMap<>();
        questionRepository.findByIdIn(solvedInRange.stream().map(UserQuestionProgress::getQuestionId).distinct().toList())
                .forEach(q -> questionsById.put(q.getId(), q));

        Map<LocalDate, List<UserQuestionProgress>> byDay = solvedInRange.stream()
                .collect(Collectors.groupingBy(p -> p.getSolvedAt().atZone(ZONE).toLocalDate()));

        return byDay.entrySet().stream()
                .map(entry -> CalendarDayResponse.builder()
                        .date(entry.getKey())
                        .solvedCount(entry.getValue().size())
                        .questions(entry.getValue().stream()
                                .map(p -> {
                                    Question q = questionsById.get(p.getQuestionId());
                                    return new QuestionSummary(p.getQuestionId(), q == null ? null : q.getTitle());
                                })
                                .toList())
                        .build())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentSolvedResponse> recentSolved(Long userId, int days, int limit) {
        Instant after = LocalDate.now(ZONE).minusDays(days - 1L).atStartOfDay(ZONE).toInstant();
        List<UserQuestionProgress> solved = progressRepository
                .findByUserIdAndSolvedTrueAndSolvedAtAfterOrderBySolvedAtDesc(userId, after);

        Map<Long, Question> questionsById = new HashMap<>();
        questionRepository.findByIdIn(solved.stream().map(UserQuestionProgress::getQuestionId).distinct().toList())
                .forEach(q -> questionsById.put(q.getId(), q));

        Map<Long, Topic> topicsById = new HashMap<>();
        topicRepository.findAllById(questionsById.values().stream().map(Question::getTopicId).distinct().toList())
                .forEach(t -> topicsById.put(t.getId(), t));

        return solved.stream()
                .limit(limit)
                .map(p -> {
                    Question q = questionsById.get(p.getQuestionId());
                    Topic topic = q == null ? null : topicsById.get(q.getTopicId());
                    return RecentSolvedResponse.builder()
                            .questionId(p.getQuestionId())
                            .title(q == null ? null : q.getTitle())
                            .topicName(topic == null ? null : topic.getName())
                            .solvedAt(p.getSolvedAt())
                            .build();
                })
                .toList();
    }

    private int countSolvedOn(Long userId, LocalDate date) {
        Instant from = date.atStartOfDay(ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
        return progressRepository.findByUserIdAndSolvedTrueAndSolvedAtBetween(userId, from, to).size();
    }

    /**
     * Solves R = w*rate + e*(2*rate - 2) for rate, i.e. rate = ceil((R + 2e) / (w + 2e)).
     */
    private static int computeBaseRate(int remaining, LocalDate from, LocalDate to) {
        if (remaining <= 0) return 0;
        if (from.isAfter(to)) return remaining; // overdue: everything due "today"
        int weekdays = 0, weekends = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) weekends++; else weekdays++;
        }
        int denom = weekdays + 2 * weekends;
        if (denom == 0) return remaining;
        return (int) Math.ceil((remaining + 2 * weekends) / (double) denom);
    }

    private static int targetForDay(int baseRate, LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        if (baseRate <= 0) return 0;
        return weekend ? Math.max(1, 2 * baseRate - 2) : baseRate;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private static Instant endOfDay(LocalDate day) {
        return day.plusDays(1).atStartOfDay(ZONE).toInstant().minusNanos(1);
    }

    // ---- Goal item feature ----

    private int solvedCountAsOf(Long userId, Set<Long> ids, Instant asOf) {
        if (ids.isEmpty()) return 0;
        Set<Long> solvedIds = new HashSet<>(questionRepository.findSolvedQuestionIdsAsOf(userId, asOf));
        solvedIds.retainAll(ids);
        return solvedIds.size();
    }

    /**
     * completeAll mode ("solve every matching question in this topic") is a persistent, all-time
     * state — count every solve ever made, not just ones since the goal's startDate (you shouldn't
     * have to re-solve old work). Count mode ("solve N specific questions") only counts NEW solves
     * made since the goal's startDate, toward the requested number.
     */
    private int completedCountAsOf(Long userId, Set<Long> ids, LocalDate startDate, boolean completeAll, Instant asOf) {
        if (completeAll) {
            return solvedCountAsOf(userId, ids, asOf);
        }
        Instant from = startDate.atStartOfDay(ZONE).toInstant();
        List<UserQuestionProgress> solved = progressRepository.findByUserIdAndSolvedTrueAndSolvedAtBetween(userId, from, asOf);
        return (int) solved.stream().filter(p -> ids.contains(p.getQuestionId())).count();
    }

    private int completedOnDay(Long userId, Set<Long> ids, LocalDate startDate, boolean completeAll, LocalDate day) {
        if (completeAll) {
            int upTo = completedCountAsOf(userId, ids, startDate, true, endOfDay(day));
            int upToPrev = completedCountAsOf(userId, ids, startDate, true, endOfDay(day.minusDays(1)));
            return upTo - upToPrev;
        }
        Instant from = day.atStartOfDay(ZONE).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(ZONE).toInstant();
        List<UserQuestionProgress> solved = progressRepository.findByUserIdAndSolvedTrueAndSolvedAtBetween(userId, from, to);
        return (int) solved.stream().filter(p -> ids.contains(p.getQuestionId())).count();
    }

    /**
     * Computes the full per-topic response for one {@link DsaGoalTopicTarget}, given its parent
     * goal's shared {@code sheetId}/{@code startDate}/{@code endDate}. Works for both a persisted
     * target (real create/list path) and a transient, never-saved one (preview path) since it
     * never relies on the target having an id or its parent having been saved.
     */
    private DsaGoalTopicTargetResponse computeTopicTargetResponse(Long userId, DsaGoalTopicTarget target,
                                                                    Long sheetId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(ZONE);
        Set<Long> ids = questionScopeResolver.resolve(target.getTopicId(), target.getDifficulties(), sheetId);
        boolean completeAll = target.getTargetQuestionCount() == null;
        int totalTarget = completeAll ? ids.size() : target.getTargetQuestionCount();

        int completedCount = completedCountAsOf(userId, ids, startDate, completeAll, Instant.now());
        int remainingCount = Math.max(0, totalTarget - completedCount);
        int baseRate = computeBaseRate(remainingCount, today, endDate);
        int todayTargetCount = targetForDay(baseRate, today);
        int todayCompletedCount = completedOnDay(userId, ids, startDate, completeAll, today);
        boolean overdue = today.isAfter(endDate) && remainingCount > 0;

        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);
        Boolean yesterdayMet = null;
        if (!startDate.isAfter(yesterday)) {
            int completedBeforeYesterday = completedCountAsOf(userId, ids, startDate, completeAll, endOfDay(dayBeforeYesterday));
            int remainingAtStartOfYesterday = totalTarget - completedBeforeYesterday;
            if (remainingAtStartOfYesterday > 0) {
                int yesterdayBaseRate = computeBaseRate(remainingAtStartOfYesterday, yesterday, endDate);
                int yesterdayTarget = targetForDay(yesterdayBaseRate, yesterday);
                int completedThroughYesterday = completedCountAsOf(userId, ids, startDate, completeAll, endOfDay(yesterday));
                int actualYesterday = completedThroughYesterday - completedBeforeYesterday;
                yesterdayMet = actualYesterday >= yesterdayTarget;
            }
        }

        String topicName = topicRepository.findById(target.getTopicId()).map(Topic::getName).orElse(null);

        return DsaGoalTopicTargetResponse.builder()
                .topicId(target.getTopicId())
                .topicName(topicName)
                .targetQuestionCount(target.getTargetQuestionCount())
                .difficulties(target.getDifficulties())
                .totalTarget(totalTarget)
                .completedCount(completedCount)
                .remainingCount(remainingCount)
                .todayTargetCount(todayTargetCount)
                .todayCompletedCount(todayCompletedCount)
                .isWeekend(isWeekend(today))
                .overdue(overdue)
                .yesterdayMet(yesterdayMet)
                .done(remainingCount <= 0)
                .build();
    }

    private DsaGoalItemResponse toResponse(Long userId, DsaGoalItem item, List<DsaGoalTopicTarget> targets) {
        List<DsaGoalTopicTargetResponse> topicResponses = targets.stream()
                .map(t -> computeTopicTargetResponse(userId, t, item.getSheetId(), item.getStartDate(), item.getEndDate()))
                .toList();

        String sheetName = item.getSheetId() == null ? null
                : sheetRepository.findById(item.getSheetId()).map(Sheet::getName).orElse(null);

        boolean done = topicResponses.stream().allMatch(DsaGoalTopicTargetResponse::isDone);
        int overallRemaining = topicResponses.stream().mapToInt(DsaGoalTopicTargetResponse::getRemainingCount).sum();
        int overallTodayTarget = topicResponses.stream().mapToInt(DsaGoalTopicTargetResponse::getTodayTargetCount).sum();

        return DsaGoalItemResponse.builder()
                .id(item.getId())
                .sheetId(item.getSheetId())
                .sheetName(sheetName)
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .topics(topicResponses)
                .done(done)
                .overallRemainingCount(overallRemaining)
                .overallTodayTargetCount(overallTodayTarget)
                .build();
    }

    @Transactional
    public DsaGoalItemResponse createGoalItem(Long userId, DsaGoalItemRequest req) {
        LocalDate today = LocalDate.now(ZONE);
        DsaGoalItem item = DsaGoalItem.builder()
                .userId(userId)
                .sheetId(req.getSheetId())
                .startDate(today)
                .endDate(req.getEndDate())
                .build();
        DsaGoalItem saved = dsaGoalItemRepository.save(item);

        List<DsaGoalTopicTarget> targets = req.getTopics().stream()
                .map(t -> DsaGoalTopicTarget.builder()
                        .goalItemId(saved.getId())
                        .topicId(t.getTopicId())
                        .targetQuestionCount(t.getTargetQuestionCount())
                        .difficulties(t.getDifficulties() == null ? new ArrayList<>() : new ArrayList<>(t.getDifficulties()))
                        .build())
                .map(dsaGoalTopicTargetRepository::save)
                .toList();

        return toResponse(userId, saved, targets);
    }

    @Transactional(readOnly = true)
    public DsaGoalItemResponse previewGoalItem(Long userId, DsaGoalItemRequest req) {
        LocalDate today = LocalDate.now(ZONE);
        DsaGoalItem transientItem = DsaGoalItem.builder()
                .userId(userId)
                .sheetId(req.getSheetId())
                .startDate(today)
                .endDate(req.getEndDate())
                .build();

        List<DsaGoalTopicTarget> transientTargets = req.getTopics().stream()
                .map(t -> DsaGoalTopicTarget.builder()
                        .topicId(t.getTopicId())
                        .targetQuestionCount(t.getTargetQuestionCount())
                        .difficulties(t.getDifficulties() == null ? new ArrayList<>() : new ArrayList<>(t.getDifficulties()))
                        .build())
                .toList();

        return toResponse(userId, transientItem, transientTargets);
    }

    @Transactional(readOnly = true)
    public List<DsaGoalItemResponse> listGoalItems(Long userId) {
        return dsaGoalItemRepository.findByUserId(userId).stream()
                .sorted((a, b) -> a.getEndDate().compareTo(b.getEndDate()))
                .map(item -> toResponse(userId, item, dsaGoalTopicTargetRepository.findByGoalItemId(item.getId())))
                .toList();
    }

    @Transactional
    public void deleteGoalItem(Long userId, Long itemId) {
        DsaGoalItem item = dsaGoalItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal item not found: " + itemId));
        dsaGoalTopicTargetRepository.deleteByGoalItemId(item.getId());
        dsaGoalItemRepository.delete(item);
    }

    /** Edits an existing goal in place: swaps its sheet scope/deadline and replaces its
     * topic targets wholesale, but keeps the original startDate so in-progress pacing
     * (since-goal-started counts for QUESTIONS-mode targets) isn't reset by the edit. */
    @Transactional
    public DsaGoalItemResponse updateGoalItem(Long userId, Long itemId, DsaGoalItemRequest req) {
        DsaGoalItem item = dsaGoalItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal item not found: " + itemId));
        item.setSheetId(req.getSheetId());
        item.setEndDate(req.getEndDate());
        DsaGoalItem saved = dsaGoalItemRepository.save(item);

        dsaGoalTopicTargetRepository.deleteByGoalItemId(saved.getId());
        List<DsaGoalTopicTarget> targets = req.getTopics().stream()
                .map(t -> DsaGoalTopicTarget.builder()
                        .goalItemId(saved.getId())
                        .topicId(t.getTopicId())
                        .targetQuestionCount(t.getTargetQuestionCount())
                        .difficulties(t.getDifficulties() == null ? new ArrayList<>() : new ArrayList<>(t.getDifficulties()))
                        .build())
                .map(dsaGoalTopicTargetRepository::save)
                .toList();

        return toResponse(userId, saved, targets);
    }

    @Transactional(readOnly = true)
    public List<DsaGoalItemDayResponse> goalItemsSchedule(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now(ZONE);

        List<DsaGoalItem> items = dsaGoalItemRepository.findByUserId(userId).stream()
                .filter(i -> !i.getEndDate().isBefore(monthStart) && !i.getStartDate().isAfter(monthEnd))
                .toList();

        Map<LocalDate, int[]> byDay = new TreeMap<>();
        Map<LocalDate, Map<Long, int[]>> byDayTopic = new TreeMap<>();
        Set<Long> allTopicIds = new HashSet<>();

        for (DsaGoalItem item : items) {
            List<DsaGoalTopicTarget> targets = dsaGoalTopicTargetRepository.findByGoalItemId(item.getId());
            for (DsaGoalTopicTarget target : targets) {
                allTopicIds.add(target.getTopicId());
                Set<Long> ids = questionScopeResolver.resolve(target.getTopicId(), target.getDifficulties(), item.getSheetId());
                boolean completeAll = target.getTargetQuestionCount() == null;
                int totalTarget = completeAll ? ids.size() : target.getTargetQuestionCount();
                int completedNow = completedCountAsOf(userId, ids, item.getStartDate(), completeAll, Instant.now());
                int remainingNow = Math.max(0, totalTarget - completedNow);
                int baseRate = computeBaseRate(remainingNow, today, item.getEndDate());

                LocalDate rangeStart = item.getStartDate().isAfter(monthStart) ? item.getStartDate() : monthStart;
                LocalDate rangeEnd = item.getEndDate().isBefore(monthEnd) ? item.getEndDate() : monthEnd;

                for (LocalDate day = rangeStart; !day.isAfter(rangeEnd); day = day.plusDays(1)) {
                    int planned = targetForDay(baseRate, day);
                    int actual = day.isAfter(today) ? 0 : completedOnDay(userId, ids, item.getStartDate(), completeAll, day);
                    int[] agg = byDay.computeIfAbsent(day, d -> new int[2]);
                    agg[0] += planned;
                    agg[1] += actual;
                    int[] topicAgg = byDayTopic.computeIfAbsent(day, d -> new HashMap<>())
                            .computeIfAbsent(target.getTopicId(), t -> new int[2]);
                    topicAgg[0] += planned;
                    topicAgg[1] += actual;
                }
            }
        }

        Map<Long, String> topicNameById = topicRepository.findAllById(allTopicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Topic::getName));

        return byDay.entrySet().stream()
                .map(e -> {
                    List<DsaGoalTopicDayBreakdown> topicBreakdown = byDayTopic.getOrDefault(e.getKey(), Map.of())
                            .entrySet().stream()
                            .map(te -> DsaGoalTopicDayBreakdown.builder()
                                    .topicId(te.getKey())
                                    .topicName(topicNameById.get(te.getKey()))
                                    .plannedCount(te.getValue()[0])
                                    .actualCount(te.getValue()[1])
                                    .build())
                            .toList();
                    return DsaGoalItemDayResponse.builder()
                            .date(e.getKey())
                            .plannedCount(e.getValue()[0])
                            .actualCount(e.getValue()[1])
                            .isWeekend(isWeekend(e.getKey()))
                            .topics(topicBreakdown)
                            .build();
                })
                .toList();
    }
}
