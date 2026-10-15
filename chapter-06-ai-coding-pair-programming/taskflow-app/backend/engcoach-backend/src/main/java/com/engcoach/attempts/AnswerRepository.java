package com.engcoach.attempts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /**
     * Backs the auto-save upsert in AttemptsService.saveAnswer: one Answer
     * row per (attempt, question) — found and overwritten on re-answer,
     * created on first answer.
     */
    Optional<Answer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
