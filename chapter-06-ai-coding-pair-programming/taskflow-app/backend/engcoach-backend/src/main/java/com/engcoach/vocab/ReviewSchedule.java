package com.engcoach.vocab;

import java.time.Duration;
import java.time.Instant;

/**
 * Leitner-style spaced repetition for flashcards. A word sits in a box from 1 to 5. Remembering it moves it up one
 * box and the next review waits longer; forgetting it sends it back to box 1 and it comes back soon.
 */
public final class ReviewSchedule {

    public static final int FIRST_BOX = 1;
    public static final int LAST_BOX = 5;

    /** Days to wait after the word reaches box 1..5 (index 0 is box 1, unused when remembered). */
    private static final int[] DAYS_IN_BOX = {1, 2, 4, 7, 14};

    /** A forgotten word is shown again after this delay, so it is not repeated at once in the same session. */
    static final Duration FORGOTTEN_DELAY = Duration.ofMinutes(10);

    private ReviewSchedule() {
    }

    public record Next(int box, Instant nextReviewAt) {
    }

    public static Next after(int box, boolean remembered, Instant now) {
        if (!remembered) {
            return new Next(FIRST_BOX, now.plus(FORGOTTEN_DELAY));
        }
        int newBox = Math.min(Math.max(box, FIRST_BOX) + 1, LAST_BOX);
        return new Next(newBox, now.plus(Duration.ofDays(DAYS_IN_BOX[newBox - 1])));
    }
}
