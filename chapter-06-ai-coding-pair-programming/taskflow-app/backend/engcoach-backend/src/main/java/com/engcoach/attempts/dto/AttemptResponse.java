package com.engcoach.attempts.dto;

import com.engcoach.attempts.Attempt;

import java.time.Instant;

/**
 * Response body for {@code POST /attempts} (both the "created" and the
 * "resumed existing in-progress attempt" cases return the same shape —
 * the caller can't tell which happened from the body alone, which is
 * intentional: both are a 201 with "here is your current attempt").
 */
public record AttemptResponse(
        Long id,
        String type,
        String part,
        String status,
        Instant startedAt
) {
    public static AttemptResponse from(Attempt attempt) {
        return new AttemptResponse(
                attempt.getId(),
                attempt.getType(),
                attempt.getPart(),
                attempt.getStatus(),
                attempt.getStartedAt()
        );
    }
}
