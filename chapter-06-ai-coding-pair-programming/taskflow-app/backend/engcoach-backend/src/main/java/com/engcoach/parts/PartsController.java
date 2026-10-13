package com.engcoach.parts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implements {@code GET /parts}, already declared in
 * chapter-05-ai-for-software-design-architecture/design/openapi.yaml with
 * {@code security: [bearerAuth]}. No {@code permitAll()} entry was added
 * for this path in SecurityConfig — it's protected by the existing
 * {@code anyRequest().authenticated()} default plus JwtAuthenticationFilter
 * (S3), exactly as SecurityConfig's Javadoc says every new endpoint should
 * be from here on.
 */
@RestController
public class PartsController {

    private final PartsService partsService;

    public PartsController(PartsService partsService) {
        this.partsService = partsService;
    }

    @GetMapping("/parts")
    public ResponseEntity<List<String>> listParts() {
        return ResponseEntity.ok(partsService.listParts());
    }
}
