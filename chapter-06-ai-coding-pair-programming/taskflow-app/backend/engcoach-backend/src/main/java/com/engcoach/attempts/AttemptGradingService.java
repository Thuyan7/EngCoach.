package com.engcoach.attempts;

import com.engcoach.common.AttemptAccessDeniedException;
import com.engcoach.common.AttemptNotFoundException;
import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S8a/S8b: the transactional core of submitting an attempt, split out of
 * {@link AttemptsService} as its own bean. Approved S8b design decision
 * #2: {@link AttemptsService#submitAttempt} must not hold a single DB
 * transaction open across its own slow Anthropic API calls, and a
 * {@code @Transactional} method calling another {@code @Transactional}
 * method on the SAME bean would not actually open a second transaction —
 * Spring's proxy-based {@code @Transactional} is bypassed on
 * self-invocation. So the DB work that must commit quickly (grading, and
 * later each individual explanation write) lives here, on a separate
 * bean, while {@link AttemptsService} orchestrates without wrapping the
 * whole thing in one transaction.
 *
 * S11 (approved decisions #1/#2): {@link #loadCorrectOptions} now loads
 * the full question bank for {@code MOCK_TEST} too (via
 * {@code questionRepository.findAll()}), the same "full assigned
 * question set" shape {@code PART_PRACTICE} already used — so
 * {@code totalQuestions} is computed identically for both types now,
 * and the old MOCK_TEST limitation (totalQuestions = only the answers
 * actually saved) flagged in SLICE-S8A-NOTES.md/SLICE-S8B-NOTES.md no
 * longer applies.
 */
@Service
public class AttemptGradingService {

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public AttemptGradingService(AttemptRepository attemptRepository,
                                  AnswerRepository answerRepository,
                                  QuestionRepository questionRepository) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Approved S8a design decision #4 (idempotent resubmission): if the
     * attempt is already SUBMITTED, returns its existing persisted state
     * with {@code justGraded = false} — the caller uses that flag to skip
     * generating explanations again (S8b: they were already generated, or
     * attempted, the first time this attempt was graded; a later resubmit
     * is a no-op, not a second chance at explanations — that's what the
     * dedicated retry endpoint is for).
     */
    @Transactional
    public GradingResult gradeAndPersist(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }

        if (Attempt.STATUS_SUBMITTED.equals(attempt.getStatus())) {
            return new GradingResult(attempt, answerRepository.findByAttemptId(attemptId), false);
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

        // Approved S8a decision #2, now applying to both types since S11
        // decision #1/#2: totalQuestions is the size of the full assigned
        // question set (the attempt's Part for PART_PRACTICE, the whole
        // bank for MOCK_TEST) — so an unanswered question still counts as
        // incorrect without a sentinel row, for both attempt types.
        int totalQuestions = correctOptionsByQuestionId.size();

        attempt.markSubmitted(correctCount, totalQuestions);
        attempt = attemptRepository.save(attempt);

        return new GradingResult(attempt, answers, true);
    }

    /**
     * S8b: persists one answer's AI-generated explanation result (or its
     * failure) in its own short transaction, separate from
     * {@link #gradeAndPersist} — approved decision #2, so each slow
     * Anthropic call (made by the caller, outside any transaction) only
     * blocks a single-row write afterward, not a long-lived one.
     * {@code explanationText} is null when {@code explanationStatus} is
     * {@link Answer#EXPLANATION_STATUS_UNAVAILABLE}.
     */
    @Transactional
    public void saveExplanationResult(Long answerId, String explanationText, String explanationStatus) {
        answerRepository.findById(answerId).ifPresent(answer -> {
            answer.setExplanationText(explanationText);
            answer.setExplanationStatus(explanationStatus);
            answerRepository.save(answer);
        });
    }

    /**
     * S11 (approved decisions #1/#2): {@code MOCK_TEST} now loads the
     * same shape of "full assigned question set" that
     * {@code PART_PRACTICE} always has — every {@code Question} in the
     * attempt's Part for {@code PART_PRACTICE}, every {@code Question} in
     * the whole bank for {@code MOCK_TEST} (via
     * {@code questionRepository.findAll()}) — rather than the old
     * MOCK_TEST limitation of only looking up questions that were
     * actually answered. {@code answers} is unused for grading purposes
     * now but still accepted for symmetry with the caller.
     */
    private Map<Long, String> loadCorrectOptions(Attempt attempt, List<Answer> answers) {
        Map<Long, String> correctOptionsByQuestionId = new HashMap<>();
        Iterable<Question> fullQuestionSet = "PART_PRACTICE".equals(attempt.getType())
                ? questionRepository.findByPart(attempt.getPart())
                : questionRepository.findAll();
        for (Question question : fullQuestionSet) {
            correctOptionsByQuestionId.put(question.getId(), question.getCorrectOption());
        }
        return correctOptionsByQuestionId;
    }

    /**
     * @param justGraded {@code true} only when this call actually
     *                   performed grading just now (first submit);
     *                   {@code false} when it short-circuited on an
     *                   already-SUBMITTED attempt.
     */
    public record GradingResult(Attempt attempt, List<Answer> answers, boolean justGraded) {
    }
}
