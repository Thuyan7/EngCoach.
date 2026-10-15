package com.engcoach.common;

/**
 * Thrown when an {@code /attempts/{id}} path references an attempt id that
 * doesn't exist at all (as opposed to existing but belonging to another
 * user — see AttemptAccessDeniedException for that case). Maps to 404 via
 * GlobalExceptionHandler.
 */
public class AttemptNotFoundException extends RuntimeException {

    public AttemptNotFoundException() {
        super("Attempt not found.");
    }
}
