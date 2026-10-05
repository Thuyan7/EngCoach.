package com.engcoach.common;

/**
 * Thrown by AuthService when POST /auth/register is called with an email
 * that already exists. Maps to 409 via GlobalExceptionHandler, per
 * software-architecture.md §5 ("Email already registered | 409").
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account with email '" + email + "' already exists.");
    }
}
