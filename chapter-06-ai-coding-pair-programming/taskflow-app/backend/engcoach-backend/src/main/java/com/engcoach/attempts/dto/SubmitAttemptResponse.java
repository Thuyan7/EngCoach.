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
 * S8b adds {@code explanationStatus}/{@code explanationText} to
 * {@link GradedAnswer}: implementation-plan.md's S9 ("Result screen")
 * depends only on S8a + S8b, with no separate "result" slice of its own,
 * so this submit response is the one place the Result screen gets both
 * score and explanations from — {@code GET /attempts/{id}/result}
 * (named in openapi.yaml/software-architecture.md) isn't built by this
 * slice. Both fields are {@code null} for a correct answer — no
 * explanation is ever requested for one.
 */
public record SubmitAttemptResponse(
        Long id,
        String status,
        Integer score,
        Integer totalQuestions,
        List<GradedAnswer> answers
) {
    public record GradedAnswer(Long questionId, String chosenOption, boolean isCorrect,
                                String explanationStatus, String explanationText) {
        public static GradedAnswer from(Answer answer) {
            return new GradedAnswer(answer.getQuestionId(), answer.getChosenOption(),
                    Boolean.TRUE.equals(answer.getIsCorrect()),
                    answer.getExplanationStatus(), answer.getExplanationText());
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
