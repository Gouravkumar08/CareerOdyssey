package com.outreach.dsa.controller;

import com.outreach.dsa.dto.CalendarDayResponse;
import com.outreach.dsa.dto.DsaGoalItemDayResponse;
import com.outreach.dsa.dto.DsaGoalItemRequest;
import com.outreach.dsa.dto.DsaGoalItemResponse;
import com.outreach.dsa.dto.RecentSolvedResponse;
import com.outreach.dsa.dto.TargetPlanRequest;
import com.outreach.dsa.dto.TargetPlanResponse;
import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.dsa.service.PlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dsa/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/target")
    public TargetPlanResponse getTarget(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.getTarget(currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/target")
    public TargetPlanResponse setTarget(@Valid @RequestBody TargetPlanRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.setTarget(currentUserResolver.resolve(userIdHeader), request);
    }

    @GetMapping("/calendar")
    public List<CalendarDayResponse> calendar(@RequestParam int year, @RequestParam int month,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.calendar(currentUserResolver.resolve(userIdHeader), year, month);
    }

    @GetMapping("/recent")
    public List<RecentSolvedResponse> recent(@RequestParam(defaultValue = "2") int days,
                                              @RequestParam(defaultValue = "10") int limit,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.recentSolved(currentUserResolver.resolve(userIdHeader), days, limit);
    }

    @PostMapping("/goal-items")
    public DsaGoalItemResponse createGoalItem(@Valid @RequestBody DsaGoalItemRequest request,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.createGoalItem(currentUserResolver.resolve(userIdHeader), request);
    }

    @PostMapping("/goal-items/preview")
    public DsaGoalItemResponse previewGoalItem(@Valid @RequestBody DsaGoalItemRequest request,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.previewGoalItem(currentUserResolver.resolve(userIdHeader), request);
    }

    @GetMapping("/goal-items")
    public List<DsaGoalItemResponse> listGoalItems(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.listGoalItems(currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/goal-items/{id}")
    public DsaGoalItemResponse updateGoalItem(@PathVariable Long id,
                                               @Valid @RequestBody DsaGoalItemRequest request,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.updateGoalItem(currentUserResolver.resolve(userIdHeader), id, request);
    }

    @DeleteMapping("/goal-items/{id}")
    public void deleteGoalItem(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        plannerService.deleteGoalItem(currentUserResolver.resolve(userIdHeader), id);
    }

    @GetMapping("/goal-items/schedule")
    public List<DsaGoalItemDayResponse> goalItemsSchedule(@RequestParam int year, @RequestParam int month,
                                                           @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.goalItemsSchedule(currentUserResolver.resolve(userIdHeader), year, month);
    }
}
