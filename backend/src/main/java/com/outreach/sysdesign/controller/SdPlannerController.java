package com.outreach.sysdesign.controller;

import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.sysdesign.dto.SdCalendarDayResponse;
import com.outreach.sysdesign.dto.SdGoalItemDayResponse;
import com.outreach.sysdesign.dto.SdGoalItemRequest;
import com.outreach.sysdesign.dto.SdGoalItemResponse;
import com.outreach.sysdesign.dto.SdRecentCompletedResponse;
import com.outreach.sysdesign.dto.SdTargetPlanRequest;
import com.outreach.sysdesign.dto.SdTargetPlanResponse;
import com.outreach.sysdesign.service.SdPlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sysdesign/planner")
@RequiredArgsConstructor
public class SdPlannerController {

    private final SdPlannerService plannerService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/target")
    public SdTargetPlanResponse getTarget(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.getTarget(currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/target")
    public SdTargetPlanResponse setTarget(@Valid @RequestBody SdTargetPlanRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.setTarget(currentUserResolver.resolve(userIdHeader), request);
    }

    @GetMapping("/calendar")
    public List<SdCalendarDayResponse> calendar(@RequestParam int year, @RequestParam int month,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.calendar(currentUserResolver.resolve(userIdHeader), year, month);
    }

    @GetMapping("/recent")
    public List<SdRecentCompletedResponse> recent(@RequestParam(defaultValue = "2") int days,
                                                   @RequestParam(defaultValue = "10") int limit,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.recentSolved(currentUserResolver.resolve(userIdHeader), days, limit);
    }

    @PostMapping("/goal-items")
    public SdGoalItemResponse createGoalItem(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                              @Valid @RequestBody SdGoalItemRequest req) {
        return plannerService.createGoalItem(currentUserResolver.resolve(userIdHeader), req);
    }

    @GetMapping("/goal-items")
    public List<SdGoalItemResponse> listGoalItems(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return plannerService.listGoalItems(currentUserResolver.resolve(userIdHeader));
    }

    @PostMapping("/goal-items/preview")
    public SdGoalItemResponse previewGoalItem(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                               @Valid @RequestBody SdGoalItemRequest req) {
        return plannerService.previewGoalItem(currentUserResolver.resolve(userIdHeader), req);
    }

    @PutMapping("/goal-items/{id}")
    public SdGoalItemResponse updateGoalItem(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                              @PathVariable Long id,
                                              @Valid @RequestBody SdGoalItemRequest req) {
        return plannerService.updateGoalItem(currentUserResolver.resolve(userIdHeader), id, req);
    }

    @DeleteMapping("/goal-items/{id}")
    public void deleteGoalItem(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                @PathVariable Long id) {
        plannerService.deleteGoalItem(currentUserResolver.resolve(userIdHeader), id);
    }

    @GetMapping("/goal-items/schedule")
    public List<SdGoalItemDayResponse> goalItemsSchedule(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                                           @RequestParam int year, @RequestParam int month) {
        return plannerService.goalItemsSchedule(currentUserResolver.resolve(userIdHeader), year, month);
    }
}
