package com.engcoach.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService's S3 addition, validateAndGetUserId(). The
 * issuance side (issueAccessToken) is already covered indirectly by
 * AuthRegistrationTest/AuthLoginTest; these focus on the failure modes a
 * JwtAuthenticationFilter needs to treat as "not authenticated" rather than
 * crash on.
 */
class JwtServiceTest {

    // Matches src/test/resources/application.yml's engcoach.jwt.secret, so
    // a JwtService built with this constant round-trips tokens the same way
    // the Spring-managed bean would in the MockMvc-based tests.
    private static final String TEST_SECRET = "test-only-secret-not-used-in-production-32chars-min";

    private final JwtService jwtService = new JwtService(TEST_SECRET);

    @Test
    void validToken_returnsTheUserId() {
        JwtService.IssuedToken issued = jwtService.issueAccessToken(42L);

        Optional<Long> userId = jwtService.validateAndGetUserId(issued.token());

        assertThat(userId).contains(42L);
    }

    @Test
    void expiredToken_returnsEmpty() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThat(jwtService.validateAndGetUserId(expiredToken)).isEmpty();
    }

    @Test
    void tokenSignedWithWrongKey_returnsEmpty() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-also-32-chars-plus".getBytes(StandardCharsets.UTF_8));
        String tokenFromAnotherServer = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(wrongKey)
                .compact();

        assertThat(jwtService.validateAndGetUserId(tokenFromAnotherServer)).isEmpty();
    }

    @Test
    void malformedToken_returnsEmpty() {
        assertThat(jwtService.validateAndGetUserId("not-a-jwt-at-all")).isEmpty();
    }

    @Test
    void tokenWithNonNumericSubject_returnsEmpty() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenWithBadSubject = Jwts.builder()
                .subject("not-a-user-id")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        assertThat(jwtService.validateAndGetUserId(tokenWithBadSubject)).isEmpty();
    }
}
