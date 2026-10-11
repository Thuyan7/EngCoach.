package com.engcoach.auth;

import com.engcoach.auth.dto.AuthResponse;
import com.engcoach.auth.dto.LoginRequest;
import com.engcoach.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the /auth/register and /auth/login operations already
 * declared in
 * chapter-05-ai-for-software-design-architecture/design/openapi.yaml.
 * S1 (register) + S2 (login) — see implementation-plan.md for S3 (JWT route
 * protection) onward.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
