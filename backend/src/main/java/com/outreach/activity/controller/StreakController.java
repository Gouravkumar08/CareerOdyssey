package com.outreach.activity.controller;

import com.outreach.activity.dto.StreakResponse;
import com.outreach.activity.service.StreakService;
import com.outreach.dsa.service.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/streak")
    public StreakResponse getStreak(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return streakService.getStreak(currentUserResolver.resolve(userIdHeader));
    }
}
