package com.engcoach.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates access tokens per ADR-003 (JWT Expiry Strategy):
 *  - single access token, 3-hour expiry, no refresh token for this MVP.
 *  - subject = user id ONLY (software-architecture.md §2: "No email or other
 *    personal data embedded in the token payload").
 *
 * S1: token issuance (issueAccessToken). S2 reuses it for login. S3 adds
 * validateAndGetUserId() — the JwtAuthenticationFilter is what actually
 * applies this on protected routes; this service only verifies the token
 * itself and extracts the subject.
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

    /**
     * Verifies the token's signature and expiry, and extracts the user id
     * from the subject claim.
     *
     * @return the user id if the token is structurally valid, correctly
     *         signed with this server's key, and not expired; empty for
     *         any other reason (malformed, wrong signature, expired, or a
     *         non-numeric subject). Deliberately returns Optional rather
     *         than throwing — JwtAuthenticationFilter treats every failure
     *         reason identically (skip authentication, let Spring Security's
     *         401 entry point handle it), so the distinction isn't needed
     *         by the only caller.
     */
    public Optional<Long> validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
