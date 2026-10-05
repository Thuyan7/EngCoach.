package com.engcoach.auth.dto;

import java.time.Instant;

/**
 * Response body for POST /auth/register (and, from S2 onward, /auth/login).
 *
 * Deliberately excludes id/email/passwordHash — per design-patterns.md §3
 * (DTO pattern), a response DTO is hand-picked, never the entity, so a field
 * like password_hash can never leak by accident.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt
) {
    public static AuthResponse bearer(String jwt, Instant expiresAt) {
        return new AuthResponse(jwt, "Bearer", expiresAt);
    }
}
