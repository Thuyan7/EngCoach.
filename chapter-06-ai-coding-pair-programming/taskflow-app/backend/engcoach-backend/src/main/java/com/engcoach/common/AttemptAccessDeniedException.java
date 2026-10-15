package com.engcoach.common;

/**
 * Thrown when a caller with a valid JWT targets an {@code /attempts/{id}}
 * (or nested {@code /attempts/{id}/...}) resource that exists but belongs
 * to a different user. This is the first real implementation of the
 * per-resource ownership check that SecurityConfig's Javadoc (S3) and
 * JwtProtectionTest's SLICE-S3-NOTES.md flagged as deferred — JWT
 * authentication alone (401 for "no/bad token") is not the same thing as
 * ownership (403 for "valid token, wrong owner").
 *
 * Maps to 403 via GlobalExceptionHandler, matching
 * chapter-05-ai-for-software-design-architecture/design/openapi.yaml's
 * documented "403: Attempt belongs to another user" response.
 */
public class AttemptAccessDeniedException extends RuntimeException {

    public AttemptAccessDeniedException() {
        super("This attempt belongs to another user.");
    }
}
