package com.engcoach.attempts.dto;

import com.engcoach.attempts.Answer;
import com.engcoach.attempts.Attempt;

import java.util.List;

/**
 * Response body for {@code POST /attempts/{id}/submit} (S8a).
 *
 * {@code score} is a raw count of correctly answered questions, not a
 * scaled TOEIC score (0-990) — that scaled conversion is a separate,
 * still-open product question (PRD OQ-004), not addressed here. Approved
 * S8a design decision #1: {@code totalQuestions} is returned alongside
 * {@code score} so the caller can compute a percentage itself.
 *
 * {@code explanationText}/{@code explanationStatus} are deliberately
 * absent from {@link GradedAnswer} — that's S8b's AI-explanation scope,
 * not this slice's.
 */
public record SubmitAttemptResponse(
        Long id,
        String status,
        Integer score,
        Integer totalQuestions,
        List<GradedAnswer> answers
) {
    public record GradedAnswer(Long questionId, String chosenOption, boolean isCorrect) {
        public static GradedAnswer from(Answer answer) {
            return new GradedAnswer(answer.getQuestionId(), answer.getChosenOption(),
                    Boolean.TRUE.equals(answer.getIsCorrect()));
        }
    }

    public static SubmitAttemptResponse from(Attempt attempt, List<Answer> answers) {
        return new SubmitAttemptResponse(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                answers.stream().map(GradedAnswer::from).toList()
        );
    }
}
