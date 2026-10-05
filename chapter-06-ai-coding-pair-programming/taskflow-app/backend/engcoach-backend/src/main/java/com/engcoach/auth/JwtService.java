package com.engcoach.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues access tokens per ADR-003 (JWT Expiry Strategy):
 *  - single access token, 3-hour expiry, no refresh token for this MVP.
 *  - subject = user id ONLY (software-architecture.md §2: "No email or other
 *    personal data embedded in the token payload").
 *
 * S1 scope: token issuance only. Validating this token on protected routes
 * (JwtAuthenticationFilter) is S3 in implementation-plan.md — not built yet.
 */
@Service
public class JwtService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(3);

    private final SecretKey signingKey;

    public JwtService(@Value("${engcoach.jwt.secret}") String jwtSecret) {
        // Raw UTF-8 secret, must be >= 32 characters (256 bits) for HS256.
        // See application.yml / README for how this is supplied in each
        // environment (env var in Railway, local-only default for dev).
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "engcoach.jwt.secret must be at least 32 characters (256 bits) for HS256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param userId the authenticated user's id — becomes the JWT subject.
     *               Never pass email or any other personal data here.
     */
    public IssuedToken issueAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ACCESS_TOKEN_TTL);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
