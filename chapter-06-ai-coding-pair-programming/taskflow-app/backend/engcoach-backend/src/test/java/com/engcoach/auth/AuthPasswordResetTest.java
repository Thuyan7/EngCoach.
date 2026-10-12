package com.engcoach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice S4. No email provider exists in this project (see
 * SLICE-S4-NOTES.md), so AuthService logs the raw reset token instead of
 * emailing it — these tests capture stdout (Spring Boot's
 * OutputCaptureExtension) to recover that token the same way a dev would
 * read it from the console in this MVP stand-in. This is test-only
 * plumbing around a known, documented limitation, not something
 * production code relies on.
 *
 * Only the token's SHA-256 hash is ever persisted (PasswordResetToken);
 * these tests only ever see the raw value via the log, same as a real
 * caller would via a real email — they never read the hash column.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuthPasswordResetTest {

    private static final Pattern TOKEN_LOG_PATTERN = Pattern.compile("reset token = (\\S+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void registerUser(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
    }

    private String extractLatestToken(CapturedOutput output) {
        Matcher matcher = TOKEN_LOG_PATTERN.matcher(output.getAll());
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        assertThat(lastMatch).as("a reset token should have been logged").isNotNull();
        return lastMatch;
    }

    /** Mirrors AuthService's private hashToken(), to build a token row directly in tests. */
    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void forgotPassword_withRegisteredEmail_returns200AndLogsAResetToken(CapturedOutput output) throws Exception {
        registerUser("reset-me@example.com", "correct-horse-battery");

        String body = objectMapper.writeValueAsString(Map.of("email", "reset-me@example.com"));
        mockMvc.perform(post("/auth/forgot-password").contentType("application/json").content(body))
                .andExpect(status().isOk());

        extractLatestToken(output); // throws if nothing was logged
    }

    @Test
    void forgotPassword_withUnknownEmail_returns200SameAsRegisteredEmail_andPersistsNoToken() throws Exception {
        long before = passwordResetTokenRepository.count();

        String body = objectMapper.writeValueAsString(Map.of("email", "never-signed-up@example.com"));
        mockMvc.perform(post("/auth/forgot-password").contentType("application/json").content(body))
                .andExpect(status().isOk());

        // Same 200 as a registered email (feature-specification.md §3: no
        // way to tell from the response whether the account exists), and no
        // token row should have been created for an email nobody owns.
        assertThat(passwordResetTokenRepository.count()).isEqualTo(before);
    }

    @Test
    void resetPassword_withValidToken_changesPasswordAndOldPasswordStopsWorking(CapturedOutput output)
            throws Exception {
        registerUser("full-flow@example.com", "old-password-123");

        String forgotBody = objectMapper.writeValueAsString(Map.of("email", "full-flow@example.com"));
        mockMvc.perform(post("/auth/forgot-password").contentType("application/json").content(forgotBody))
                .andExpect(status().isOk());
        String rawToken = extractLatestToken(output);

        String resetBody = objectMapper.writeValueAsString(Map.of(
                "token", rawToken,
                "newPassword", "brand-new-password-456"
        ));
        mockMvc.perform(post("/auth/reset-password").contentType("application/json").content(resetBody))
                .andExpect(status().isOk());

        // New password logs in.
        String loginWithNew = objectMapper.writeValueAsString(Map.of(
                "email", "full-flow@example.com", "password", "brand-new-password-456"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginWithNew))
                .andExpect(status().isOk());

        // Old password no longer works.
        String loginWithOld = objectMapper.writeValueAsString(Map.of(
                "email", "full-flow@example.com", "password", "old-password-123"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginWithOld))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_withSameTokenUsedTwice_secondAttemptReturns410(CapturedOutput output) throws Exception {
        registerUser("single-use@example.com", "old-password-123");

        String forgotBody = objectMapper.writeValueAsString(Map.of("email", "single-use@example.com"));
        mockMvc.perform(post("/auth/forgot-password").contentType("application/json").content(forgotBody))
                .andExpect(status().isOk());
        String rawToken = extractLatestToken(output);

        String resetBody = objectMapper.writeValueAsString(Map.of(
                "token", rawToken, "newPassword", "second-new-password-789"));

        mockMvc.perform(post("/auth/reset-password").contentType("application/json").content(resetBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/reset-password").contentType("application/json").content(resetBody))
                .andExpect(status().isGone())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid or expired reset token"));
    }

    @Test
    void resetPassword_withUnknownToken_returns410() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "token", "this-token-was-never-issued", "newPassword", "whatever-password-123"));

        mockMvc.perform(post("/auth/reset-password").contentType("application/json").content(body))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.title").value("Invalid or expired reset token"));
    }

    @Test
    void resetPassword_withExpiredToken_returns410() throws Exception {
        registerUser("expired-token@example.com", "old-password-123");
        Long userId = userRepository.findByEmailIgnoreCase("expired-token@example.com")
                .orElseThrow()
                .getId();

        // Built directly (hashed the same way AuthService does), since
        // there's no way to fast-forward a real issued token's clock from
        // outside the service.
        String rawToken = "manually-issued-raw-token-for-expiry-test";
        passwordResetTokenRepository.save(
                new PasswordResetToken(sha256Hex(rawToken), userId, Instant.now().minusSeconds(60)));

        String body = objectMapper.writeValueAsString(Map.of(
                "token", rawToken, "newPassword", "whatever-password-123"));

        mockMvc.perform(post("/auth/reset-password").contentType("application/json").content(body))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.title").value("Invalid or expired reset token"));
    }

    @Test
    void resetPassword_withTooShortNewPassword_returns422_andTokenRemainsUnused() throws Exception {
        registerUser("short-password@example.com", "old-password-123");

        Long userId = userRepository.findByEmailIgnoreCase("short-password@example.com")
                .orElseThrow()
                .getId();
        String rawToken = "manually-issued-raw-token-for-validation-test";
        String tokenHash = sha256Hex(rawToken);
        passwordResetTokenRepository.save(
                new PasswordResetToken(tokenHash, userId, Instant.now().plusSeconds(1800)));

        String body = objectMapper.writeValueAsString(Map.of("token", rawToken, "newPassword", "short"));

        mockMvc.perform(post("/auth/reset-password").contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());

        // Bean validation runs before AuthService.resetPassword() is ever
        // called, so a rejected request must not burn the token — it should
        // still be usable afterward.
        PasswordResetToken stored = passwordResetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(stored.getUsedAt()).isNull();
    }
}
