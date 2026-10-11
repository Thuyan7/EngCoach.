package com.engcoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /auth/reset-password. Mirrors the
 * "token, newPassword" schema in openapi.yaml for this endpoint.
 *
 * newPassword reuses RegisterRequest's 8-character minimum (same policy,
 * same validation-error shape: 422 via GlobalExceptionHandler) — no new
 * password rule invented for this slice.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "token must not be blank")
        String token,

        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, message = "newPassword must be at least 8 characters")
        String newPassword
) {
}
