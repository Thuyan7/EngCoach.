package com.engcoach.common;

/**
 * Thrown by AttemptsService.startAttempt() when a Part practice attempt is
 * requested for a {@code part} that has zero {@code Question} rows. This
 * is approved S6a design decision #3: don't create an empty, unplayable
 * {@code Attempt} — reject it up front instead. Maps to 422 via
 * GlobalExceptionHandler.
 *
 * S11 reuses this same exception, via the no-arg constructor, when a
 * {@code MOCK_TEST} is started with zero {@code Question} rows in the
 * entire bank (approved S11 design decision #3) — same rationale, no new
 * exception type needed for the "mock test" flavor of this check.
 */
public class PartHasNoQuestionsException extends RuntimeException {

    public PartHasNoQuestionsException(String part) {
        super("Part '" + part + "' has no questions available yet.");
    }

    public PartHasNoQuestionsException() {
        super("No questions are available yet for a mock test.");
    }
}
