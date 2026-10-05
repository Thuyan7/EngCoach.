package com.engcoach.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /auth/register.
 * Mirrors the "email, password" schema already declared in
 * chapter-05-ai-for-software-design-architecture/design/openapi.yaml
 * for this endpoint — not changing the contract, just implementing it.
 */
public record RegisterRequest(

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {
}
