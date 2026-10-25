package com.engcoach.common;

/**
 * S8b: thrown by {@code AttemptsService.retryExplanation} when no
 * {@code Answer} row exists for the given (attemptId, questionId) pair —
 * either the question was never answered in this attempt, or the
 * questionId doesn't belong to this attempt at all. Maps to 404 via
 * GlobalExceptionHandler, same pattern as AttemptNotFoundException.
 */
public class AnswerNotFoundException extends RuntimeException {

    public AnswerNotFoundException() {
        super("No answer found for this attempt and question.");
    }
}
