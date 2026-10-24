package com.engcoach.attempts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /**
     * Backs the auto-save upsert in AttemptsService.saveAnswer: one Answer
     * row per (attempt, question) — found and overwritten on re-answer,
     * created on first answer.
     */
    Optional<Answer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    /**
     * Backs S7a's {@code GET /attempts/{id}}: every previously saved answer
     * for an attempt, restoring exact in-progress state on resume.
     */
    List<Answer> findByAttemptId(Long attemptId);

    /**
     * S10a, approved decision #6: {@code Attempt}/{@code Answer} have no
     * JPA {@code @OneToMany} relationship (only a plain FK column), so
     * deleting an {@code Attempt} doesn't cascade on its own — this is
     * called explicitly by {@code AttemptsService.deleteAttempt} before
     * deleting the {@code Attempt} row itself, per
     * software-architecture.md §3 ("Deleting an Attempt cascades to its
     * Answer rows").
     */
    void deleteByAttemptId(Long attemptId);
}
