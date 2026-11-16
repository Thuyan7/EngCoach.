package com.engcoach.goals;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request and response bodies of the study goals API. */
public final class GoalsDtos {

    private GoalsDtos() {
    }

    /** {@code targetScore} is optional (null clears it); the daily goal is always set. */
    public record UpdateGoalsRequest(
            @Min(10) @Max(990) Integer targetScore,
            @NotNull @Min(1) @Max(500) Integer dailyQuestionGoal) {
    }

    /**
     * {@code estimatedScore} is the learner's latest estimated TOEIC total, or null if they have not finished an
     * attempt covering Listening and Reading yet.
     */
    public record GoalsResponse(
            Integer targetScore,
            int dailyQuestionGoal,
            int answeredToday,
            boolean dailyGoalMet,
            int currentStreak,
            int longestStreak,
            Integer estimatedScore) {
    }
}
