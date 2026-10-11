package com.engcoach.common;

/**
 * Thrown by AuthService.resetPassword() when the token doesn't exist, has
 * already been used, or has expired. Maps to 410 via GlobalExceptionHandler,
 * per openapi.yaml ("410: Token expired or already used") and
 * feature-specification.md §3 ("expired/used link -> clear error").
 *
 * Deliberately one exception/message for all three causes (not found vs.
 * used vs. expired) — same rationale as InvalidCredentialsException: the
 * holder of a dead token doesn't need to know which specific way it died.
 */
public class InvalidOrExpiredResetTokenException extends RuntimeException {

    public InvalidOrExpiredResetTokenException() {
        super("This password reset link is invalid or has expired.");
    }
}
