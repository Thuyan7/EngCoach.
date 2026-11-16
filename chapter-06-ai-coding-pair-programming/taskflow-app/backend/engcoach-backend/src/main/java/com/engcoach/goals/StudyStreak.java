package com.engcoach.goals;

import java.time.LocalDate;
import java.util.Set;

/**
 * Study streak: consecutive calendar days on which the learner answered at least one question.
 * A streak is still alive if the learner has not studied yet today but did yesterday.
 */
public final class StudyStreak {

    private StudyStreak() {
    }

    public record Result(int current, int longest) {
    }

    public static Result compute(Set<LocalDate> studyDays, LocalDate today) {
        if (studyDays.isEmpty()) {
            return new Result(0, 0);
        }

        int longest = 0;
        for (LocalDate day : studyDays) {
            // Count a run only from its first day, so every run is measured once.
            if (studyDays.contains(day.minusDays(1))) continue;
            int length = 1;
            while (studyDays.contains(day.plusDays(length))) length++;
            longest = Math.max(longest, length);
        }

        LocalDate last = studyDays.contains(today) ? today : today.minusDays(1);
        int current = 0;
        while (studyDays.contains(last.minusDays(current))) current++;
        return new Result(current, longest);
    }
}
