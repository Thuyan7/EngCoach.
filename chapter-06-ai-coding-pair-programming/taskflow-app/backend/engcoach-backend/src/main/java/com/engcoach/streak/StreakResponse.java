package com.engcoach.streak;

import java.util.List;

/**
 * Study streak and the study days of one month, for the streak calendar. Dates are ISO strings (yyyy-MM-dd) in the
 * learner's time zone; {@code month} is yyyy-MM.
 */
public record StreakResponse(
        int currentStreak,
        int longestStreak,
        boolean studiedToday,
        String today,
        String month,
        List<String> studyDays) {
}
