package com.engcoach.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * S4: supports "Password Reset" (feature-specification.md §3). Maps 1:1 to
 * the {@code PASSWORD_RESET_TOKEN} entity in
 * chapter-05-ai-for-software-design-architecture/design/data-model.mmd.
 *
 * Only {@code tokenHash} is persisted, never the raw token — same reasoning
 * as {@code User.passwordHash}: a DB read/leak must not hand out a token
 * that still works. (An earlier version of this class stored the raw token
 * directly; corrected to match the approved data model's {@code token_hash}
 * column before this was merged — see SLICE-S4-NOTES.md.)
 *
 * "tokens single-use, time-limited" (feature-spec §3, Persistence) is
 * enforced by {@code usedAt} (null until consumed, then immutable) and
 * {@code expiresAt}; AuthService checks both before accepting a token.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordResetToken() {
        // required by JPA
    }

    public PasswordResetToken(String tokenHash, Long userId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
