package com.engcoach.goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A learner's study goals: an optional target TOEIC score and a daily question goal. One row per user. */
@Entity
@Table(name = "user_goals")
public class UserGoal {

    public static final int DEFAULT_DAILY_QUESTION_GOAL = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "target_score")
    private Integer targetScore;

    @Column(name = "daily_question_goal", nullable = false)
    private int dailyQuestionGoal;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserGoal() {
        // required by JPA
    }

    public UserGoal(Long userId, Integer targetScore, int dailyQuestionGoal) {
        this.userId = userId;
        this.targetScore = targetScore;
        this.dailyQuestionGoal = dailyQuestionGoal;
        this.updatedAt = Instant.now();
    }

    public void update(Integer targetScore, int dailyQuestionGoal) {
        this.targetScore = targetScore;
        this.dailyQuestionGoal = dailyQuestionGoal;
        this.updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTargetScore() {
        return targetScore;
    }

    public int getDailyQuestionGoal() {
        return dailyQuestionGoal;
    }
}
