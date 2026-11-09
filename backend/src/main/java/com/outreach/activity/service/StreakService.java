package com.outreach.activity.service;

import com.outreach.activity.dto.StreakResponse;
import com.outreach.dsa.repository.UserQuestionProgressRepository;
import com.outreach.sysdesign.repository.UserSdItemProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserQuestionProgressRepository questionProgressRepository;
    private final UserSdItemProgressRepository sdProgressRepository;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Transactional(readOnly = true)
    public StreakResponse getStreak(Long userId) {
        Set<LocalDate> activeDays = new HashSet<>();
        questionProgressRepository.findByUserIdAndSolvedTrue(userId).forEach(p -> {
            if (p.getSolvedAt() != null) {
                activeDays.add(p.getSolvedAt().atZone(ZONE).toLocalDate());
            }
        });
        sdProgressRepository.findByUserIdAndCompletedTrue(userId).forEach(p -> {
            if (p.getCompletedAt() != null) {
                activeDays.add(p.getCompletedAt().atZone(ZONE).toLocalDate());
            }
        });

        LocalDate today = LocalDate.now(ZONE);
        LocalDate mostRecent = activeDays.stream().max(LocalDate::compareTo).orElse(null);

        int streak = 0;
        // A gap of at most one day (i.e. the last active day was today or yesterday) keeps the
        // streak alive; anything older means it already broke, so it reports zero.
        if (mostRecent != null && ChronoUnit.DAYS.between(mostRecent, today) <= 1) {
            LocalDate cursor = mostRecent;
            while (activeDays.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            }
        }

        // A single active day isn't "consistency" yet — that only starts once the user comes
        // back and does it again on the very next day, so a lone day reports as no streak.
        if (streak < 2) {
            streak = 0;
        }

        String message = streak > 0
                ? "You've been consistent for " + streak + " day" + (streak == 1 ? "" : "s") + "!"
                : "";

        return StreakResponse.builder().currentStreak(streak).message(message).build();
    }
}
