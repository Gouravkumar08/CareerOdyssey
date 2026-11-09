package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdCalendarDayResponse;
import com.outreach.sysdesign.dto.SdGoalItemDayResponse;
import com.outreach.sysdesign.dto.SdGoalItemRequest;
import com.outreach.sysdesign.dto.SdGoalItemResponse;
import com.outreach.sysdesign.dto.SdGoalTopicDayBreakdown;
import com.outreach.sysdesign.dto.SdGoalTopicStatus;
import com.outreach.sysdesign.dto.SdItemSummary;
import com.outreach.sysdesign.dto.SdRecentCompletedResponse;
import com.outreach.sysdesign.dto.SdTargetPlanRequest;
import com.outreach.sysdesign.dto.SdTargetPlanResponse;
import com.outreach.sysdesign.exception.SdResourceNotFoundException;
import com.outreach.sysdesign.model.SdGoalItem;
import com.outreach.sysdesign.model.SdItem;
import com.outreach.sysdesign.model.SdSheet;
import com.outreach.sysdesign.model.SdSheetItem;
import com.outreach.sysdesign.model.SdTargetPlan;
import com.outreach.sysdesign.model.SdTopic;
import com.outreach.sysdesign.model.UserSdItemProgress;
import com.outreach.sysdesign.repository.SdGoalItemRepository;
import com.outreach.sysdesign.repository.SdItemRepository;
import com.outreach.sysdesign.repository.SdSheetItemRepository;
import com.outreach.sysdesign.repository.SdSheetRepository;
import com.outreach.sysdesign.repository.SdTargetPlanRepository;
import com.outreach.sysdesign.repository.SdTopicRepository;
import com.outreach.sysdesign.repository.UserSdItemProgressRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SdPlannerService {

    private final SdTargetPlanRepository targetPlanRepository;
    private final UserSdItemProgressRepository progressRepository;
    private final SdItemRepository itemRepository;
    private final SdTopicRepository topicRepository;
    private final SdSheetItemRepository sdSheetItemRepository;
    private final SdGoalItemRepository sdGoalItemRepository;
    private final SdSheetRepository sdSheetRepository;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Transactional(readOnly = true)
    public SdTargetPlanResponse getTarget(Long userId) {
        SdTargetPlan plan = targetPlanRepository.findByUserId(userId).orElse(null);
        int todayCompleted = countCompletedOn(userId, LocalDate.now(ZONE));
        if (plan == null) {
            return SdTargetPlanResponse.builder().dailyTargetCount(0).sheetId(null).startDate(null).todayCompletedCount(todayCompleted).build();
        }
        return SdTargetPlanResponse.builder()
                .dailyTargetCount(plan.getDailyTargetCount())
                .sheetId(plan.getSheetId())
                .startDate(plan.getStartDate())
                .todayCompletedCount(todayCompleted)
                .build();
    }

    @Transactional
    public SdTargetPlanResponse setTarget(Long userId, SdTargetPlanRequest request) {
        SdTargetPlan plan = targetPlanRepository.findByUserId(userId)
                .orElseGet(() -> SdTargetPlan.builder().userId(userId).startDate(LocalDate.now(ZONE)).build());
        plan.setDailyTargetCount(request.getDailyTargetCount());
        plan.setSheetId(request.getSheetId());
        SdTargetPlan saved = targetPlanRepository.save(plan);
        return SdTargetPlanResponse.builder()
                .dailyTargetCount(saved.getDailyTargetCount())
                .sheetId(saved.getSheetId())
                .startDate(saved.getStartDate())
                .todayCompletedCount(countCompletedOn(userId, LocalDate.now(ZONE)))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SdCalendarDayResponse> calendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        Instant from = yearMonth.atDay(1).atStartOfDay(ZONE).toInstant();
        Instant to = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZONE).toInstant();

        List<UserSdItemProgress> completedInRange = progressRepository.findByUserIdAndCompletedTrueAndCompletedAtBetween(userId, from, to);
        Map<Long, SdItem> itemsById = new HashMap<>();
        itemRepository.findByIdIn(completedInRange.stream().map(UserSdItemProgress::getItemId).distinct().toList())
                .forEach(i -> itemsById.put(i.getId(), i));

        Map<LocalDate, List<UserSdItemProgress>> byDay = completedInRange.stream()
                .collect(Collectors.groupingBy(p -> p.getCompletedAt().atZone(ZONE).toLocalDate()));

        return byDay.entrySet().stream()
                .map(entry -> {
                    long distinctTopics = entry.getValue().stream()
                            .map(p -> itemsById.get(p.getItemId()))
                            .filter(java.util.Objects::nonNull)
                            .map(SdItem::getTopicId)
                            .distinct()
                            .count();
                    return SdCalendarDayResponse.builder()
                            .date(entry.getKey())
                            .completedCount(entry.getValue().size())
                            .topicsCount((int) distinctTopics)
                            .items(entry.getValue().stream()
                                    .map(p -> {
                                        SdItem i = itemsById.get(p.getItemId());
                                        return new SdItemSummary(p.getItemId(), i == null ? null : i.getTitle());
                                    })
                                    .toList())
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SdRecentCompletedResponse> recentSolved(Long userId, int days, int limit) {
        Instant after = LocalDate.now(ZONE).minusDays(days - 1L).atStartOfDay(ZONE).toInstant();
        List<UserSdItemProgress> completed = progressRepository
                .findByUserIdAndCompletedTrueAndCompletedAtAfterOrderByCompletedAtDesc(userId, after);

        Map<Long, SdItem> itemsById = new HashMap<>();
        itemRepository.findByIdIn(completed.stream().map(UserSdItemProgress::getItemId).distinct().toList())
                .forEach(i -> itemsById.put(i.getId(), i));

        Map<Long, SdTopic> topicsById = new HashMap<>();
        topicRepository.findAllById(itemsById.values().stream().map(SdItem::getTopicId).distinct().toList())
                .forEach(t -> topicsById.put(t.getId(), t));

        return completed.stream()
                .limit(limit)
                .map(p -> {
                    SdItem i = itemsById.get(p.getItemId());
                    SdTopic topic = i == null ? null : topicsById.get(i.getTopicId());
                    return SdRecentCompletedResponse.builder()
                            .itemId(p.getItemId())
                            .title(i == null ? null : i.getTitle())
                            .topicName(topic == null ? null : topic.getName())
                            .completedAt(p.getCompletedAt())
                            .build();
                })
                .toList();
    }

    private int countCompletedOn(Long userId, LocalDate date) {
        Instant from = date.atStartOfDay(ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
        return progressRepository.findByUserIdAndCompletedTrueAndCompletedAtBetween(userId, from, to).size();
    }

    // ---- Goal item feature ----

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

    private static boolean isWeekend(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private static Instant startOfDay(LocalDate day) {
        return day.atStartOfDay(ZONE).toInstant();
    }

    /**
     * A topic's target/progress is measured in ITEMS (every item tagged with that topic,
     * optionally restricted to one sheet) — not in "number of topics" — so the daily
     * redistribution math produces a meaningful item count instead of collapsing to a tiny
     * 0/1/2 range. Each topic in a goal is computed independently: no pooling across topics.
     */
    private Set<Long> goalItemIds(Long topicId, Long sheetId) {
        Set<Long> sheetItemIds = sheetId == null ? null : sdSheetItemRepository.findBySheetIdOrderByOrderIndexAsc(sheetId).stream()
                .map(SdSheetItem::getItemId)
                .collect(Collectors.toSet());
        Set<Long> ids = new HashSet<>();
        for (SdItem sdItem : itemRepository.findByTopicId(topicId)) {
            if (sheetItemIds == null || sheetItemIds.contains(sdItem.getId())) {
                ids.add(sdItem.getId());
            }
        }
        return ids;
    }

    /** Count of the given item ids completed as of the given instant — topic completion is a
     *  persistent, all-time state, so this ignores the goal's startDate entirely. */
    private int completedCount(Long userId, Set<Long> itemIds, Instant asOf) {
        if (itemIds.isEmpty()) return 0;
        Set<Long> completedItemIds = new HashSet<>(itemRepository.findCompletedItemIdsAsOf(userId, asOf));
        completedItemIds.retainAll(itemIds);
        return completedItemIds.size();
    }

    @Transactional
    public SdGoalItemResponse createGoalItem(Long userId, SdGoalItemRequest req) {
        SdGoalItem item = SdGoalItem.builder()
                .userId(userId)
                .topicIds(new ArrayList<>(req.getTopicIds()))
                .sheetId(req.getSheetId())
                .startDate(LocalDate.now(ZONE))
                .endDate(req.getEndDate())
                .build();
        SdGoalItem saved = sdGoalItemRepository.save(item);
        return toResponse(saved, userId);
    }

    @Transactional(readOnly = true)
    public List<SdGoalItemResponse> listGoalItems(Long userId) {
        return sdGoalItemRepository.findByUserId(userId).stream()
                .map(item -> toResponse(item, userId))
                .toList();
    }

    @Transactional
    public void deleteGoalItem(Long userId, Long itemId) {
        SdGoalItem item = sdGoalItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new SdResourceNotFoundException("Goal item not found: " + itemId));
        sdGoalItemRepository.delete(item);
    }

    /** Edits an existing goal in place: swaps its topics/sheet scope/deadline, but keeps the
     * original startDate so pacing math isn't reset by the edit. */
    @Transactional
    public SdGoalItemResponse updateGoalItem(Long userId, Long itemId, SdGoalItemRequest req) {
        SdGoalItem item = sdGoalItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new SdResourceNotFoundException("Goal item not found: " + itemId));
        item.setTopicIds(new ArrayList<>(req.getTopicIds()));
        item.setSheetId(req.getSheetId());
        item.setEndDate(req.getEndDate());
        SdGoalItem saved = sdGoalItemRepository.save(item);
        return toResponse(saved, userId);
    }

    @Transactional(readOnly = true)
    public List<SdGoalItemDayResponse> goalItemsSchedule(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now(ZONE);

        List<SdGoalItem> items = sdGoalItemRepository.findByUserId(userId).stream()
                .filter(it -> !it.getEndDate().isBefore(monthStart) && !it.getStartDate().isAfter(monthEnd))
                .toList();

        Map<LocalDate, int[]> byDay = new HashMap<>(); // [planned, actual]
        Map<LocalDate, Map<Long, int[]>> byDayTopic = new HashMap<>();
        Set<Long> allTopicIds = new HashSet<>();

        for (SdGoalItem item : items) {
            for (Long topicId : item.getTopicIds()) {
                allTopicIds.add(topicId);
                Set<Long> itemIds = goalItemIds(topicId, item.getSheetId());
                int totalTarget = itemIds.size();
                int completedCount = completedCount(userId, itemIds, Instant.now());
                int remainingCount = Math.max(0, totalTarget - completedCount);
                int baseRate = computeBaseRate(remainingCount, today, item.getEndDate());

                LocalDate rangeStart = monthStart.isAfter(item.getStartDate()) ? monthStart : item.getStartDate();
                LocalDate rangeEnd = monthEnd.isBefore(item.getEndDate()) ? monthEnd : item.getEndDate();

                for (LocalDate day = rangeStart; !day.isAfter(rangeEnd); day = day.plusDays(1)) {
                    int plannedCount = targetForDay(baseRate, day);
                    int actualCount = day.isAfter(today) ? 0
                            : completedCount(userId, itemIds, startOfDay(day.plusDays(1)))
                            - completedCount(userId, itemIds, startOfDay(day));
                    int[] slot = byDay.computeIfAbsent(day, d -> new int[2]);
                    slot[0] += plannedCount;
                    slot[1] += actualCount;
                    int[] topicSlot = byDayTopic.computeIfAbsent(day, d -> new HashMap<>())
                            .computeIfAbsent(topicId, t -> new int[2]);
                    topicSlot[0] += plannedCount;
                    topicSlot[1] += actualCount;
                }
            }
        }

        Map<Long, String> topicNameById = topicRepository.findAllById(allTopicIds).stream()
                .collect(Collectors.toMap(SdTopic::getId, SdTopic::getName));

        return byDay.entrySet().stream()
                .filter(e -> e.getValue()[0] != 0 || e.getValue()[1] != 0)
                .map(e -> {
                    List<SdGoalTopicDayBreakdown> topicBreakdown = byDayTopic.getOrDefault(e.getKey(), Map.of())
                            .entrySet().stream()
                            .map(te -> SdGoalTopicDayBreakdown.builder()
                                    .topicId(te.getKey())
                                    .topicName(topicNameById.get(te.getKey()))
                                    .plannedCount(te.getValue()[0])
                                    .actualCount(te.getValue()[1])
                                    .build())
                            .toList();
                    return SdGoalItemDayResponse.builder()
                            .date(e.getKey())
                            .plannedCount(e.getValue()[0])
                            .actualCount(e.getValue()[1])
                            .isWeekend(isWeekend(e.getKey()))
                            .topics(topicBreakdown)
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    /** Independent per-topic target/progress computation — shared by the persisted-goal
     *  response ({@link #toResponse}) and the transient {@link #previewGoalItem} path. */
    private SdGoalTopicStatus computeTopicStatus(Long userId, Long topicId, Long sheetId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate yesterday = today.minusDays(1);

        Set<Long> itemIds = goalItemIds(topicId, sheetId);
        int totalTarget = itemIds.size();
        int completedCountNow = completedCount(userId, itemIds, Instant.now());
        int remainingCount = Math.max(0, totalTarget - completedCountNow);
        int baseRate = computeBaseRate(remainingCount, today, endDate);
        int todayTargetCount = targetForDay(baseRate, today);
        boolean overdue = today.isAfter(endDate) && remainingCount > 0;

        int completedAsOfStartOfToday = completedCount(userId, itemIds, startOfDay(today));
        int todayCompletedCount = completedCountNow - completedAsOfStartOfToday;

        Boolean yesterdayMet;
        if (startDate.isAfter(yesterday)) {
            yesterdayMet = null;
        } else {
            int completedBeforeYesterday = completedCount(userId, itemIds, startOfDay(yesterday));
            int remainingAtStartOfYesterday = totalTarget - completedBeforeYesterday;
            if (remainingAtStartOfYesterday <= 0) {
                yesterdayMet = null;
            } else {
                int yesterdayBaseRate = computeBaseRate(remainingAtStartOfYesterday, yesterday, endDate);
                int yesterdayTarget = targetForDay(yesterdayBaseRate, yesterday);
                int actualYesterday = completedAsOfStartOfToday - completedBeforeYesterday;
                yesterdayMet = actualYesterday >= yesterdayTarget;
            }
        }

        String topicName = topicRepository.findById(topicId).map(SdTopic::getName).orElse(null);

        return SdGoalTopicStatus.builder()
                .topicId(topicId)
                .topicName(topicName)
                .totalTarget(totalTarget)
                .completedCount(completedCountNow)
                .remainingCount(remainingCount)
                .todayTargetCount(todayTargetCount)
                .todayCompletedCount(todayCompletedCount)
                .isWeekend(isWeekend(today))
                .overdue(overdue)
                .yesterdayMet(yesterdayMet)
                .done(remainingCount <= 0)
                .build();
    }

    private SdGoalItemResponse buildResponse(Long id, Long userId, List<Long> topicIds, Long sheetId,
                                              LocalDate startDate, LocalDate endDate) {
        List<SdGoalTopicStatus> topics = topicIds.stream()
                .map(topicId -> computeTopicStatus(userId, topicId, sheetId, startDate, endDate))
                .toList();

        String sheetName = sheetId == null ? null
                : sdSheetRepository.findById(sheetId).map(SdSheet::getName).orElse(null);

        return SdGoalItemResponse.builder()
                .id(id)
                .sheetId(sheetId)
                .sheetName(sheetName)
                .startDate(startDate)
                .endDate(endDate)
                .topics(topics)
                .done(topics.stream().allMatch(SdGoalTopicStatus::isDone))
                .overallRemainingCount(topics.stream().mapToInt(SdGoalTopicStatus::getRemainingCount).sum())
                .overallTodayTargetCount(topics.stream().mapToInt(SdGoalTopicStatus::getTodayTargetCount).sum())
                .build();
    }

    private SdGoalItemResponse toResponse(SdGoalItem item, Long userId) {
        return buildResponse(item.getId(), userId, item.getTopicIds(), item.getSheetId(), item.getStartDate(), item.getEndDate());
    }

    /** Transient preview (never persisted) of what a goal would look like if created today
     *  with the given topics/sheet/end-date, using the exact same per-topic computation as
     *  the real create path. */
    @Transactional(readOnly = true)
    public SdGoalItemResponse previewGoalItem(Long userId, SdGoalItemRequest req) {
        return buildResponse(null, userId, req.getTopicIds(), req.getSheetId(), LocalDate.now(ZONE), req.getEndDate());
    }
}
