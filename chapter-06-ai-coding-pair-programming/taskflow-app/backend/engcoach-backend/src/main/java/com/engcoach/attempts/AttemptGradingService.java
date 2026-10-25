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

        // Approved decision #2 (totalQuestions, carried over from S8a):
        // PART_PRACTICE counts every Question in the Part, so an
        // unanswered one still counts as incorrect without a sentinel
        // row; MOCK_TEST is limited to answers actually saved until S11.
        int totalQuestions = "PART_PRACTICE".equals(attempt.getType())
                ? correctOptionsByQuestionId.size()
                : answers.size();

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

    /**
     * @param justGraded {@code true} only when this call actually
     *                   performed grading just now (first submit);
     *                   {@code false} when it short-circuited on an
     *                   already-SUBMITTED attempt.
     */
    public record GradingResult(Attempt attempt, List<Answer> answers, boolean justGraded) {
    }
}
