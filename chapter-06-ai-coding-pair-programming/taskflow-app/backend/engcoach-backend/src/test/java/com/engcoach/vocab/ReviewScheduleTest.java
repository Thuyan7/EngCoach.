package com.engcoach.vocab;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewScheduleTest {

    private static final Instant NOW = Instant.parse("2026-11-18T03:00:00Z");

    @Test
    void rememberingMovesTheWordUpAndWaitsLonger() {
        assertEquals(new ReviewSchedule.Next(2, NOW.plus(Duration.ofDays(2))), ReviewSchedule.after(1, true, NOW));
        assertEquals(new ReviewSchedule.Next(3, NOW.plus(Duration.ofDays(4))), ReviewSchedule.after(2, true, NOW));
        assertEquals(new ReviewSchedule.Next(4, NOW.plus(Duration.ofDays(7))), ReviewSchedule.after(3, true, NOW));
        assertEquals(new ReviewSchedule.Next(5, NOW.plus(Duration.ofDays(14))), ReviewSchedule.after(4, true, NOW));
    }

    @Test
    void theLastBoxStaysTheLastBox() {
        assertEquals(new ReviewSchedule.Next(5, NOW.plus(Duration.ofDays(14))), ReviewSchedule.after(5, true, NOW));
    }

    @Test
    void forgettingSendsTheWordBackToBoxOneToComeBackSoon() {
        for (int box = 1; box <= 5; box++) {
            assertEquals(new ReviewSchedule.Next(1, NOW.plus(Duration.ofMinutes(10))), ReviewSchedule.after(box, false, NOW));
        }
    }

    @Test
    void anOutOfRangeBoxIsTreatedAsTheFirstBox() {
        assertEquals(2, ReviewSchedule.after(0, true, NOW).box());
    }
}
