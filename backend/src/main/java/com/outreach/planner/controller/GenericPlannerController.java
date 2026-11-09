package com.outreach.planner.controller;

import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.planner.dto.PlannerCalendarDayResponse;
import com.outreach.planner.dto.PlannerNoteRequest;
import com.outreach.planner.dto.PlannerNoteResponse;
import com.outreach.planner.dto.PlannerOverviewResponse;
import com.outreach.planner.dto.PlannerProgressRequest;
import com.outreach.planner.dto.PlannerTaskRequest;
import com.outreach.planner.dto.PlannerTaskResponse;
import com.outreach.planner.service.PlannerNoteService;
import com.outreach.planner.service.GenericPlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class GenericPlannerController {

    private final GenericPlannerService genericPlannerService;
    private final PlannerNoteService noteService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/overview")
    public PlannerOverviewResponse overview(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return genericPlannerService.overview(currentUserResolver.resolve(userIdHeader));
    }

    @GetMapping("/calendar")
    public List<PlannerCalendarDayResponse> calendar(@RequestParam int year, @RequestParam int month,
                                                      @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return genericPlannerService.calendar(currentUserResolver.resolve(userIdHeader), year, month);
    }

    @GetMapping("/tasks")
    public List<PlannerTaskResponse> listTasks(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return genericPlannerService.listTasks(currentUserResolver.resolve(userIdHeader));
    }

    @PostMapping("/tasks")
    public PlannerTaskResponse createTask(@Valid @RequestBody PlannerTaskRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return genericPlannerService.createTask(currentUserResolver.resolve(userIdHeader), request);
    }

    @PutMapping("/tasks/{id}")
    public PlannerTaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody PlannerTaskRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return genericPlannerService.updateTask(currentUserResolver.resolve(userIdHeader), id, request);
    }

    @DeleteMapping("/tasks/{id}")
    public void deleteTask(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        genericPlannerService.deleteTask(currentUserResolver.resolve(userIdHeader), id);
    }

    @PutMapping("/tasks/{id}/completions/{date}")
    public void complete(@PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        genericPlannerService.complete(currentUserResolver.resolve(userIdHeader), id, date);
    }

    @DeleteMapping("/tasks/{id}/completions/{date}")
    public void uncomplete(@PathVariable Long id, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        genericPlannerService.uncomplete(currentUserResolver.resolve(userIdHeader), id, date);
    }

    @PatchMapping("/tasks/{id}/progress")
    public PlannerTaskResponse updateProgress(@PathVariable Long id, @Valid @RequestBody PlannerProgressRequest request,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return genericPlannerService.updateProgress(currentUserResolver.resolve(userIdHeader), id, request);
    }

    @GetMapping("/notes")
    public List<PlannerNoteResponse> listNotes(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return noteService.list(currentUserResolver.resolve(userIdHeader));
    }

    @PostMapping("/notes")
    public PlannerNoteResponse createNote(@Valid @RequestBody PlannerNoteRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return noteService.create(currentUserResolver.resolve(userIdHeader), request);
    }

    @PutMapping("/notes/{id}")
    public PlannerNoteResponse updateNote(@PathVariable Long id, @Valid @RequestBody PlannerNoteRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return noteService.update(currentUserResolver.resolve(userIdHeader), id, request);
    }

    @DeleteMapping("/notes/{id}")
    public void deleteNote(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        noteService.delete(currentUserResolver.resolve(userIdHeader), id);
    }
}
