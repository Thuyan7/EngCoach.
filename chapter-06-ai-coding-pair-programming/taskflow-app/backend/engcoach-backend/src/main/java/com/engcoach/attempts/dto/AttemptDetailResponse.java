package com.engcoach.attempts.dto;

import com.engcoach.attempts.Answer;
import com.engcoach.attempts.Attempt;

import java.time.Instant;
import java.util.List;

/**
 * Response body for {@code GET /attempts/{id}} (S7a). Restores exact
 * in-progress state: every previously saved answer, plus
 * {@code remainingSeconds} computed server-side (per ADR-003's
 * server-computed-time principle — the client is never trusted to report
 * its own remaining time).
 *
 * {@code remainingSeconds} is {@code null} when the attempt has no
 * {@code timeLimitSeconds} — approved S7a design decision #2. That is
 * every {@code PART_PRACTICE} attempt (untimed by design). Every
 * {@code MOCK_TEST} attempt has a fixed 7200-second limit since S11, so
 * it gets a real, server-computed, non-null value instead. {@code null}
 * means "untimed," not "0 seconds left" — those are different things and
 * must not be conflated.
 */
public record AttemptDetailResponse(
        Long id,
        String type,
        String part,
        String status,
        Instant startedAt,
        Long remainingSeconds,
        List<SavedAnswer> answers,
        List<QuestionView> questions
) {
    public record SavedAnswer(Long questionId, String chosenOption) {
        public static SavedAnswer from(Answer answer) {
            return new SavedAnswer(answer.getQuestionId(), answer.getChosenOption());
        }
    }

    public static AttemptDetailResponse from(Attempt attempt, Long remainingSeconds, List<Answer> answers,
                                              List<QuestionView> questions) {
        return new AttemptDetailResponse(
                attempt.getId(),
                attempt.getType(),
                attempt.getPart(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                remainingSeconds,
                answers.stream().map(SavedAnswer::from).toList(),
                questions
        );
    }
}
