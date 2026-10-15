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
 * introducing an enum for a single current value is premature.
 * {@code timeLimitSeconds} and {@code score} are mapped here because
 * they're part of the approved entity, but nothing in this slice sets
 * them: {@code timeLimitSeconds} is mock-test-only (S11), {@code score} is
 * written at S8a (scoring).
 */
@Entity
@Table(name = "attempts")
public class Attempt {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
