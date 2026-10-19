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
 * every {@code PART_PRACTICE} attempt (untimed by design) and, for now,
 * every {@code MOCK_TEST} attempt too, since S6a doesn't set
 * {@code timeLimitSeconds} yet (S11's scope). {@code null} means
 * "untimed," not "0 seconds left" — those are different things and must
 * not be conflated.
 */
public record AttemptDetailResponse(
        Long id,
        String type,
        String part,
        String status,
        Instant startedAt,
        Long remainingSeconds,
        List<SavedAnswer> answers
) {
    public record SavedAnswer(Long questionId, String chosenOption) {
        public static SavedAnswer from(Answer answer) {
            return new SavedAnswer(answer.getQuestionId(), answer.getChosenOption());
        }
    }

    public static AttemptDetailResponse from(Attempt attempt, Long remainingSeconds, List<Answer> answers) {
        return new AttemptDetailResponse(
                attempt.getId(),
                attempt.getType(),
                attempt.getPart(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                remainingSeconds,
                answers.stream().map(SavedAnswer::from).toList()
        );
    }
}
