package com.engcoach.common;

/**
 * Thrown by AttemptsService.startAttempt() when a Part practice attempt is
 * requested for a {@code part} that has zero {@code Question} rows. This
 * is approved S6a design decision #3: don't create an empty, unplayable
 * {@code Attempt} — reject it up front instead. Maps to 422 via
 * GlobalExceptionHandler.
 */
public class PartHasNoQuestionsException extends RuntimeException {

    public PartHasNoQuestionsException(String part) {
        super("Part '" + part + "' has no questions available yet.");
    }
}
