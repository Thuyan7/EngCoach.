package com.engcoach.attempts;

import com.engcoach.attempts.dto.AnswerResponse;
import com.engcoach.attempts.dto.AttemptDetailResponse;
import com.engcoach.attempts.dto.AttemptResponse;
import com.engcoach.attempts.dto.SaveAnswerRequest;
import com.engcoach.attempts.dto.StartAttemptRequest;
import com.engcoach.attempts.dto.SubmitAttemptResponse;
import com.engcoach.common.AttemptAccessDeniedException;
import com.engcoach.common.AttemptNotFoundException;
import com.engcoach.common.PartHasNoQuestionsException;
import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 * Slice S8a adds {@link #submitAttempt}: grading. See SLICE-S8A-NOTES.md
 * for that slice's approved design decisions (raw-count score plus a
 * persisted totalQuestions, unanswered-counts-as-incorrect, idempotent
 * resubmission, MOCK_TEST's known limitation until S11).
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

    /**
     * S8a: {@code POST /attempts/{id}/submit}. Approved design decision #4
     * (idempotent resubmission): once {@code status} is
     * {@code SUBMITTED}, this method never regrades — it returns exactly
     * the {@code score}/{@code totalQuestions} {@link Attempt#markSubmitted}
     * persisted the first time, built from the {@code Answer} rows as they
     * stand now (an answer saved after submission, via the still-open
     * auto-save endpoint, shows up in the list but was never counted
     * toward {@code score} and never will be by this method).
     */
    @Transactional
    public SubmitAttemptResponse submitAttempt(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }

        if (Attempt.STATUS_SUBMITTED.equals(attempt.getStatus())) {
            return SubmitAttemptResponse.from(attempt, answerRepository.findByAttemptId(attemptId));
        }

        List<Answer> answers = answerRepository.findByAttemptId(attemptId);
        Map<Long, String> correctOptionsByQuestionId = loadCorrectOptions(attempt, answers);

        int correctCount = 0;
        for (Answer answer : answers) {
            String correctOption = correctOptionsByQuestionId.get(answer.getQuestionId());
            boolean isCorrect = correctOption != null && correctOption.equals(answer.getChosenOption());
            answer.setIsCorrect(isCorrect);
            answerRepository.save(answer);
            if (isCorrect) {
                correctCount++;
            }
        }

        // Approved decision #2: totalQuestions for PART_PRACTICE is every
        // Question in the Part, not just the ones answered — an unanswered
        // question has no Answer row at all, so it simply never
        // contributes to correctCount, which is how "unanswered counts as
        // incorrect" falls out naturally without a sentinel row.
        //
        // Approved decision #3 (flagged limitation): MOCK_TEST has no real
        // assigned question set until S11, so totalQuestions there is the
        // count of answers actually saved — the best this slice can do,
        // not a claim that it's the true size of a mock test.
        int totalQuestions = "PART_PRACTICE".equals(attempt.getType())
                ? correctOptionsByQuestionId.size()
                : answers.size();

        attempt.markSubmitted(correctCount, totalQuestions);
        attempt = attemptRepository.save(attempt);

        return SubmitAttemptResponse.from(attempt, answers);
    }

    /**
     * Builds the questionId -> correctOption lookup used to grade
     * {@code answers}. For {@code PART_PRACTICE} this is every Question in
     * the attempt's Part (so unanswered ones are still represented, for
     * {@code totalQuestions}); for {@code MOCK_TEST} it's only the
     * questions actually answered, looked up individually, since no full
     * question set exists yet (S11).
     */
    private Map<Long, String> loadCorrectOptions(Attempt attempt, List<Answer> answers) {
        Map<Long, String> correctOptionsByQuestionId = new HashMap<>();
        if ("PART_PRACTICE".equals(attempt.getType())) {
            for (Question question : questionRepository.findByPart(attempt.getPart())) {
                correctOptionsByQuestionId.put(question.getId(), question.getCorrectOption());
            }
        } else {
            for (Answer answer : answers) {
                questionRepository.findById(answer.getQuestionId())
                        .ifPresent(question -> correctOptionsByQuestionId.put(question.getId(), question.getCorrectOption()));
            }
        }
        return correctOptionsByQuestionId;
    }
}
