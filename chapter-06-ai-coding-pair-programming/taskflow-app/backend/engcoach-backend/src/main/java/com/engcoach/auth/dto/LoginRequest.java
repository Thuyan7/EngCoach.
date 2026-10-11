package com.engcoach.auth.dto;

/**
 * Request body for POST /auth/login.
 * Mirrors the "email, password" schema already declared in
 * chapter-05-ai-for-software-design-architecture/design/openapi.yaml for
 * this endpoint.
 *
 * Deliberately has no bean validation constraints: per
 * feature-specification.md §2 ("Failure: wrong email/password → generic
 * error, no field disclosure") and the contract (only 200/401 documented,
 * no 422), a blank or malformed field is just another case of "wrong
 * credentials" and must fall into the same generic 401 as every other
 * failure — not a field-level validation error that would disclose which
 * field was wrong.
 */
public record LoginRequest(
        String email,
        String password
) {
}
