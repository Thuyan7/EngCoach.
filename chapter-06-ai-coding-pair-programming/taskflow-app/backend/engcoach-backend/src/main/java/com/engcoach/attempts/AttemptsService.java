package com.engcoach.attempts;

import com.engcoach.attempts.dto.AnswerResponse;
import com.engcoach.attempts.dto.AttemptDetailResponse;
import com.engcoach.attempts.dto.AttemptResponse;
import com.engcoach.attempts.dto.SaveAnswerRequest;
import com.engcoach.attempts.dto.StartAttemptRequest;
import com.engcoach.common.AttemptAccessDeniedException;
import com.engcoach.common.AttemptNotFoundException;
import com.engcoach.common.PartHasNoQuestionsException;
import com.engcoach.parts.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Slice S6a: starting a Part practice (or mock test) attempt, and
 * auto-saving one answer during it. Implements the 5 design decisions
 * approved for this slice (see SLICE-S6A-NOTES.md):
 *
 * 1. Resume-by-(user, type, part): starting an attempt first looks for an
 *    existing {@code IN_PROGRESS} one matching the same user/type/part and
 *    returns that instead of creating a duplicate.
 * 2. {@code chosenOption} requestBody on the auto-save endpoint.
 * 3. {@code POST /attempts} for a Part with zero questions is rejected
 *    (422) rather than creating an empty, unplayable attempt.
 * 4. Ownership check on the auto-save endpoint: the attempt must belong to
 *    the caller, else 403 — the first real enforcement of the ownership
 *    rule SecurityConfig/JwtProtectionTest flagged as deferred since S3.
 * 5. {@code Attempt.status} is the plain string {@code "IN_PROGRESS"} for
 *    now; no enum until a second status value ({@code SUBMITTED}, S8a)
 *    actually exists.
 *
 * Slice S7a adds {@link #getAttemptDetail}: resuming/inspecting an attempt
 * with its saved answers and server-computed remaining time (see
 * SLICE-S7A-NOTES.md for that slice's approved design decisions).
 */
@Service
public class AttemptsService {

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public AttemptsService(AttemptRepository attemptRepository,
                            AnswerRepository answerRepository,
                            QuestionRepository questionRepository) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public AttemptResponse startAttempt(Long userId, StartAttemptRequest request) {
        String type = request.getType();
        String part = "PART_PRACTICE".equals(type) ? request.getPart() : null;

        Attempt existing = attemptRepository
                .findFirstByUserIdAndTypeAndPartAndStatus(userId, type, part, Attempt.STATUS_IN_PROGRESS)
                .orElse(null);
        if (existing != null) {
            return AttemptResponse.from(existing);
        }

        if ("PART_PRACTICE".equals(type) && !questionRepository.existsByPart(part)) {
            throw new PartHasNoQuestionsException(part);
        }

        Attempt attempt = new Attempt(userId, type, part, Attempt.STATUS_IN_PROGRESS, Instant.now());
        attempt = attemptRepository.save(attempt);
        return AttemptResponse.from(attempt);
    }

    @Transactional
    public AnswerResponse saveAnswer(Long userId, Long attemptId, Long questionId, SaveAnswerRequest request) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }

        Answer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElse(null);
        if (answer == null) {
            answer = new Answer(attemptId, questionId, request.getChosenOption());
        } else {
            answer.setChosenOption(request.getChosenOption());
        }
        answer = answerRepository.save(answer);

        return AnswerResponse.from(answer);
    }

    /**
     * S7a: {@code GET /attempts/{id}}. Not gated on {@code status} —
     * approved design decision #3 — so it works for any attempt the caller
     * owns, not only {@code IN_PROGRESS} ones; limiting it to in-progress
     * attempts only, or to viewing submitted ones, is a later slice's call
     * (S10a), not a rule invented here.
     */
    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetail(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }

        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        Long remainingSeconds = computeRemainingSeconds(attempt);

        return AttemptDetailResponse.from(attempt, remainingSeconds, answers);
    }

    /**
     * Approved S7a design decision #2: {@code null} when the attempt has no
     * {@code timeLimitSeconds} (every {@code PART_PRACTICE} attempt, and
     * every {@code MOCK_TEST} attempt until S11 starts setting it) — "no
     * limit" is not the same as "0 seconds left." When a limit does exist,
     * remaining time is always computed server-side from
     * {@code startedAt}, per ADR-003: the client's own clock is never
     * trusted.
     */
    private Long computeRemainingSeconds(Attempt attempt) {
        Integer timeLimitSeconds = attempt.getTimeLimitSeconds();
        if (timeLimitSeconds == null) {
            return null;
        }
        long elapsedSeconds = Duration.between(attempt.getStartedAt(), Instant.now()).getSeconds();
        return Math.max(0L, timeLimitSeconds - elapsedSeconds);
    }
}
