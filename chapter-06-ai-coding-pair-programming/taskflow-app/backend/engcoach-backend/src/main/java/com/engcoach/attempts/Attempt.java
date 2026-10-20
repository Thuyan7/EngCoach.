package com.engcoach.attempts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps to the {@code Attempt} entity in
 * chapter-05-ai-for-software-design-architecture/design/data-model.mmd.
 * Fields: id, user_id, type, part, status, started_at,
 * time_limit_seconds, score, created_at.
 *
 * S6a scope only: writes {@code userId}, {@code type}, {@code part},
 * {@code status}, {@code startedAt}, {@code createdAt}. {@code status} is a
 * plain string ("IN_PROGRESS") for now, per the approved S6a design
 * decision — no enum yet, since "SUBMITTED" only appears at S8a and
 * introducing an enum for a single current value is premature. (S8a does
 * add the second value, {@link #STATUS_SUBMITTED}, but still as a plain
 * string — two concrete values still doesn't justify an enum on its own,
 * and changing representation is a larger, separate decision.)
 * {@code timeLimitSeconds} is mapped here because it's part of the
 * approved entity, but nothing written so far sets it:
 * {@code timeLimitSeconds} is mock-test-only (S11).
 *
 * S8a adds {@code totalQuestions} — <strong>not</strong> part of the
 * original data-model.mmd ATTEMPT entity. Approved S8a design decision #4
 * ("submitting twice is a no-op — the second call returns the exact score
 * computed by the first, never recomputed") requires persisting the
 * denominator alongside {@code score} at submit time, because
 * recomputing it on a later call could disagree with the first call's
 * value (most concretely for MOCK_TEST, whose "total" is the count of
 * saved answers until S11 assigns a real question set — more answers can
 * be saved after submission since this slice doesn't block the auto-save
 * endpoint post-submit). Logged here, and in SLICE-S8A-NOTES.md and
 * data-model.mmd, as a small schema addition that follows the same
 * precedent as S4's PASSWORD_RESET_TOKEN addition during the Chapter 5
 * architecture review — a genuine gap the approved entity didn't cover,
 * filled with the minimum needed, not invented beyond it.
 */
@Entity
@Table(name = "attempts")
public class Attempt {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SUBMITTED = "SUBMITTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column
    private String part;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column
    private Integer score;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Attempt() {
        // required by JPA
    }

    public Attempt(Long userId, String type, String part, String status, Instant startedAt) {
        this.userId = userId;
        this.type = type;
        this.part = part;
        this.status = status;
        this.startedAt = startedAt;
        this.createdAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getPart() {
        return part;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * S8a: records the one-time grading result and flips
     * {@code status} to {@link #STATUS_SUBMITTED}. Package-private — only
     * {@link AttemptsService#submitAttempt} calls this, exactly once per
     * attempt (approved decision #4: a later submit call short-circuits in
     * the service before ever reaching here, so this method itself doesn't
     * need to guard against being called twice).
     */
    void markSubmitted(int score, int totalQuestions) {
        this.status = STATUS_SUBMITTED;
        this.score = score;
        this.totalQuestions = totalQuestions;
    }
}
