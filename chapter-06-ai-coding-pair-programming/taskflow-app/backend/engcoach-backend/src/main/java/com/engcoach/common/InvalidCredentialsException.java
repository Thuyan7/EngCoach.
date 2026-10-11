package com.engcoach.common;

/**
 * Thrown by AuthService.login() whenever the email doesn't exist or the
 * password doesn't match. Deliberately carries one generic message for
 * both cases — maps to 401 via GlobalExceptionHandler, per
 * feature-specification.md §2 ("generic error, no field disclosure") and
 * software-architecture.md §5.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
