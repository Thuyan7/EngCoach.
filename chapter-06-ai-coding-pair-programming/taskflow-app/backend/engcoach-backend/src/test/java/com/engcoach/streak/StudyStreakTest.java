package com.engcoach.goals;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudyStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Test
    void noStudyDays_meansNoStreak() {
        assertEquals(new StudyStreak.Result(0, 0), StudyStreak.compute(Set.of(), TODAY));
    }

    @Test
    void studyingOnlyToday_isAStreakOfOne() {
        assertEquals(new StudyStreak.Result(1, 1), StudyStreak.compute(Set.of(TODAY), TODAY));
    }

    @Test
    void consecutiveDaysUpToToday_count() {
        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(new StudyStreak.Result(3, 3), StudyStreak.compute(days, TODAY));
    }

    @Test
    void streakStaysAliveWhenTodayIsNotStudiedYet() {
        Set<LocalDate> days = Set.of(TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(new StudyStreak.Result(2, 2), StudyStreak.compute(days, TODAY));
    }

    @Test
    void streakIsBrokenAfterAMissedDay_butTheLongestRunIsKept() {
        Set<LocalDate> days = Set.of(TODAY.minusDays(2), TODAY.minusDays(3), TODAY.minusDays(4), TODAY.minusDays(5));
        assertEquals(new StudyStreak.Result(0, 4), StudyStreak.compute(days, TODAY));
    }

    @Test
    void currentAndLongestCanDiffer() {
        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(1),
                TODAY.minusDays(10), TODAY.minusDays(11), TODAY.minusDays(12));
        assertEquals(new StudyStreak.Result(2, 3), StudyStreak.compute(days, TODAY));
    }
}
