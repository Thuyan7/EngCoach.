package com.engcoach.attempts;

import com.engcoach.ai.AnthropicExplanationService;
import com.engcoach.attempts.dto.AnswerResponse;
import com.engcoach.attempts.dto.AttemptDetailResponse;
import com.engcoach.attempts.dto.AttemptResponse;
import com.engcoach.attempts.dto.QuestionView;
import com.engcoach.attempts.dto.AttemptSummaryResponse;
import com.engcoach.attempts.dto.ExplanationResponse;
import com.engcoach.attempts.dto.SaveAnswerRequest;
import com.engcoach.attempts.dto.StartAttemptRequest;
import com.engcoach.attempts.dto.SubmitAttemptResponse;
import com.engcoach.common.AnswerNotFoundException;
import com.engcoach.common.AttemptAccessDeniedException;
import com.engcoach.common.AttemptNotFoundException;
import com.engcoach.common.ExplanationRetryNotAllowedException;
import com.engcoach.common.PartHasNoQuestionsException;
import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
 * resubmission, MOCK_TEST's known limitation until S11). The grading
 * itself now lives in {@link AttemptGradingService} (moved there by S8b,
 * see below) — this class just orchestrates it.
 *
 * Slice S8b adds AI-generated explanations for wrong answers
 * (ADR-004/{@link AnthropicExplanationService}). See SLICE-S8B-NOTES.md
 * for that slice's approved design decisions. Key point: this class is
 * deliberately NOT {@code @Transactional} on {@link #submitAttempt} or
 * {@link #retryExplanation} anymore — both call out to the slow, external
 * Anthropic API, and holding a DB transaction open for that whole time
 * would tie up a connection for no reason (approved decision #2). All DB
 * writes those two methods need happen through {@link AttemptGradingService},
 * a separate bean whose own methods are each individually
 * {@code @Transactional} and short-lived.
 *
 * Slice S10a adds {@link #listHistory} and {@link #deleteAttempt}. See
 * SLICE-S10A-NOTES.md for that slice's approved design decisions
 * (history is SUBMITTED-only, most-recent-first by startedAt, delete is
 * not status-gated, Answer rows are deleted explicitly since Attempt/
 * Answer have no JPA cascade relationship).
 */
@Service
public class AttemptsService {

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AttemptGradingService attemptGradingService;
    private final AnthropicExplanationService explanationService;

    public AttemptsService(AttemptRepository attemptRepository,
                            AnswerRepository answerRepository,
                            QuestionRepository questionRepository,
                            AttemptGradingService attemptGradingService,
                            AnthropicExplanationService explanationService) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.attemptGradingService = attemptGradingService;
        this.explanationService = explanationService;
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

        // S7b: a PART_PRACTICE attempt's questions are every question of its
        // Part (the same set S8a grades against). A MOCK_TEST has no assigned
        // question set until S11, so it gets none here.
        List<QuestionView> questions = "PART_PRACTICE".equals(attempt.getType()) && attempt.getPart() != null
                ? questionRepository.findByPartOrderByIdAsc(attempt.getPart()).stream()
                        .map(QuestionView::from).toList()
                : List.of();

        return AttemptDetailResponse.from(attempt, remainingSeconds, answers, questions);
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
     * S8a: {@code POST /attempts/{id}/submit}. Approved S8a design
     * decision #4 (idempotent resubmission): once {@code status} is
     * {@code SUBMITTED}, grading never runs again — see
     * {@link AttemptGradingService#gradeAndPersist}. S8b: on the first,
     * real grading pass ({@code justGraded = true}), generates an
     * explanation for every wrong answer right after grading commits —
     * not inside the grading transaction itself (approved decision #2).
     * A resubmit of an already-SUBMITTED attempt (justGraded = false)
     * never re-generates explanations; that's what
     * {@link #retryExplanation} is for.
     */
    public SubmitAttemptResponse submitAttempt(Long userId, Long attemptId) {
        AttemptGradingService.GradingResult result = attemptGradingService.gradeAndPersist(userId, attemptId);

        if (result.justGraded()) {
            for (Answer answer : result.answers()) {
                if (Boolean.FALSE.equals(answer.getIsCorrect())) {
                    generateAndSaveExplanation(answer);
                }
            }
        }

        List<Answer> freshAnswers = answerRepository.findByAttemptId(attemptId);
        return SubmitAttemptResponse.from(result.attempt(), freshAnswers);
    }

    /**
     * S8b: {@code POST /attempts/{id}/answers/{questionId}/retry-explanation}.
     * Approved design decision #5: only re-requests the explanation for
     * one specific answer, never re-scores the attempt — distinct from
     * resubmitting (S8a's idempotent {@link #submitAttempt}, which never
     * touches explanations past the first grading pass). Allowed only
     * when the attempt is already {@code SUBMITTED} and the targeted
     * answer's current {@code explanationStatus} is
     * {@code UNAVAILABLE} — retrying an answer that already has an
     * explanation, or one that was correct and never got one, is
     * rejected (409) rather than silently no-op'd, so the caller knows
     * its request didn't do what it may have expected.
     */
    public ExplanationResponse retryExplanation(Long userId, Long attemptId, Long questionId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }
        if (!Attempt.STATUS_SUBMITTED.equals(attempt.getStatus())) {
            throw new ExplanationRetryNotAllowedException(
                    "Explanations can only be retried once the attempt has been submitted.");
        }

        Answer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(AnswerNotFoundException::new);
        if (!Answer.EXPLANATION_STATUS_UNAVAILABLE.equals(answer.getExplanationStatus())) {
            throw new ExplanationRetryNotAllowedException(
                    "Only an UNAVAILABLE explanation can be retried.");
        }

        generateAndSaveExplanation(answer);

        Answer refreshed = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(AnswerNotFoundException::new);
        return ExplanationResponse.from(refreshed);
    }

    /**
     * Calls Anthropic (outside any DB transaction) for one answer, then
     * persists the result (success or UNAVAILABLE) through
     * {@link AttemptGradingService#saveExplanationResult} — its own short
     * transaction, per approved decision #2. If the underlying
     * {@code Question} can no longer be found (shouldn't normally happen),
     * degrades straight to UNAVAILABLE without attempting a call.
     */
    private void generateAndSaveExplanation(Answer answer) {
        Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
        if (question == null) {
            attemptGradingService.saveExplanationResult(
                    answer.getId(), null, Answer.EXPLANATION_STATUS_UNAVAILABLE);
            return;
        }

        Optional<String> explanation = explanationService.generateExplanation(
                question.getPrompt(), question.getOptions(), question.getCorrectOption(), answer.getChosenOption());

        if (explanation.isPresent()) {
            attemptGradingService.saveExplanationResult(
                    answer.getId(), explanation.get(), Answer.EXPLANATION_STATUS_OK);
        } else {
            attemptGradingService.saveExplanationResult(
                    answer.getId(), null, Answer.EXPLANATION_STATUS_UNAVAILABLE);
        }
    }

    /**
     * S10a: {@code GET /attempts}. Approved design decision #1: only
     * {@code SUBMITTED} attempts are listed (feature-specification.md §7:
     * "reflects all completed attempts") — an {@code IN_PROGRESS} attempt
     * is resumed via {@code POST /attempts}, not shown here. Approved
     * decision #2: most-recent-first by {@code startedAt}. Approved
     * decision #3: no pagination at this project's scale.
     */
    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listHistory(Long userId) {
        return attemptRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, Attempt.STATUS_SUBMITTED)
                .stream()
                .map(AttemptSummaryResponse::from)
                .toList();
    }

    /**
     * S10a: {@code DELETE /attempts/{id}}. Approved decision #5: not
     * gated on {@code status} — any attempt the caller owns can be
     * deleted, {@code IN_PROGRESS} or {@code SUBMITTED}, same precedent
     * as S7a's {@code GET /attempts/{id}}. Approved decision #6: its
     * {@code Answer} rows are deleted first/explicitly, since
     * {@code Attempt}/{@code Answer} have no JPA cascade relationship
     * (software-architecture.md §3: "Deleting an Attempt cascades to its
     * Answer rows" — the cascade is implemented here, in application
     * code, not via a JPA annotation that doesn't exist on these
     * entities).
     */
    @Transactional
    public void deleteAttempt(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }

        answerRepository.deleteByAttemptId(attemptId);
        attemptRepository.delete(attempt);
    }
}
