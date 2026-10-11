package com.engcoach.auth.dto;

/**
 * Request body for POST /auth/forgot-password. Mirrors the "email" schema
 * in openapi.yaml for this endpoint.
 *
 * Deliberately has no bean validation: per feature-specification.md §3
 * ("unregistered email -> same generic confirmation message") and the
 * contract (only 200 documented, no 422), a blank/malformed email is just
 * another case that still returns the same generic 200 — not a
 * field-level validation error, which would help distinguish registered
 * from unregistered or malformed emails.
 */
public record ForgotPasswordRequest(
        String email
) {
}
