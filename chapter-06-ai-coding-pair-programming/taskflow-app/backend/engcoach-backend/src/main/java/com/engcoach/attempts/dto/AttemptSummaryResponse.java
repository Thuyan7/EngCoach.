package com.engcoach.attempts.dto;

import com.engcoach.attempts.Attempt;

import java.time.Instant;

/**
 * One row of {@code GET /attempts} (S10a, history list). Approved design
 * decision #4: a summary, not the full per-answer detail that
 * {@code GET /attempts/{id}} (S7a) returns — a history/progress list
 * shows score trend across many attempts, not individual answers, so
 * {@code score}/{@code totalQuestions} is enough per row without an
 * extra round trip.
 */
public record AttemptSummaryResponse(
        Long id,
        String type,
        String part,
        String status,
        Integer score,
        Integer totalQuestions,
        Instant startedAt
) {
    public static AttemptSummaryResponse from(Attempt attempt) {
        return new AttemptSummaryResponse(
                attempt.getId(),
                attempt.getType(),
                attempt.getPart(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getStartedAt()
        );
    }
}
