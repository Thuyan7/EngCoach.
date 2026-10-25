package com.engcoach.common;

/**
 * S8b: thrown by {@code AttemptsService.retryExplanation} when the
 * attempt isn't SUBMITTED yet, or the targeted answer's
 * {@code explanationStatus} isn't {@code UNAVAILABLE} (already has an
 * explanation, or was correct and was never given one in the first
 * place). Approved S8b design decision #5: retry exists only to recover
 * a failed explanation call, not to re-run one that already succeeded or
 * was never needed. Maps to 409 via GlobalExceptionHandler.
 */
public class ExplanationRetryNotAllowedException extends RuntimeException {

    public ExplanationRetryNotAllowedException(String message) {
        super(message);
    }
}
