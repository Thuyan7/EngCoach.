package com.engcoach.attempts;

import com.engcoach.attempts.dto.MistakeResponses.Mistake;
import com.engcoach.attempts.dto.MistakeResponses.MistakesResponse;
import com.engcoach.attempts.dto.MistakeResponses.PartCount;
import com.engcoach.attempts.dto.QuestionView;
import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import com.engcoach.tests.QuestionGroup;
import com.engcoach.tests.QuestionGroupRepository;
import com.engcoach.tests.TestQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Lists the questions a learner got wrong. A question counts as a mistake only if its most recent graded answer
 * is wrong, so a question answered correctly in a later attempt drops off the list. Questions left unanswered have
 * no saved answer and are not listed.
 */
@Service
public class MistakeService {

    static final int MAX_MISTAKES = 200;

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionGroupRepository questionGroupRepository;

    public MistakeService(AttemptRepository attemptRepository, AnswerRepository answerRepository,
                          QuestionRepository questionRepository, TestQuestionRepository testQuestionRepository,
                          QuestionGroupRepository questionGroupRepository) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.questionGroupRepository = questionGroupRepository;
    }

    /** Latest graded answer per question, newest attempt first: question id -> answer. */
    private Map<Long, Answer> latestAnswers(Long userId, Map<Long, Attempt> attemptOfAnswer) {
        List<Attempt> attempts = attemptRepository
                .findByUserIdAndStatusOrderByStartedAtDesc(userId, Attempt.STATUS_SUBMITTED);

        // attempts are newest first, so the first answer seen for a question wins
        Map<Long, Answer> latest = new LinkedHashMap<>();
        for (Attempt attempt : attempts) {
            for (Answer answer : answerRepository.findByAttemptId(attempt.getId())) {
                if (latest.putIfAbsent(answer.getQuestionId(), answer) == null) {
                    attemptOfAnswer.put(answer.getQuestionId(), attempt);
                }
            }
        }
        return latest;
    }

    /**
     * Question ids for a mistake-practice attempt: the learner's current mistakes (optionally one Part), ordered by
     * Test and position so the questions of one conversation or passage stay together, at most {@code limit}.
     */
    @Transactional(readOnly = true)
    public List<Long> practiceQuestionIds(Long userId, String part, int limit) {
        List<Long> wrongIds = latestAnswers(userId, new HashMap<>()).values().stream()
                .filter(answer -> Boolean.FALSE.equals(answer.getIsCorrect()))
                .map(Answer::getQuestionId).toList();
        Map<Long, Question> questions = new HashMap<>();
        questionRepository.findAllById(wrongIds).forEach(question -> questions.put(question.getId(), question));
        List<Long> wanted = wrongIds.stream().filter(id -> {
            Question question = questions.get(id);
            return question != null && (part == null || part.isBlank() || part.equals(question.getPart()));
        }).toList();

        Map<Long, int[]> position = new HashMap<>();
        testQuestionRepository.findByQuestionIdInOrderByTestIdAscSortOrderAsc(wanted).forEach(link ->
                position.putIfAbsent(link.getQuestionId(), new int[] {link.getTestId().intValue(), link.getSortOrder()}));
        return wanted.stream()
                .sorted(java.util.Comparator
                        .comparingInt((Long id) -> position.containsKey(id) ? position.get(id)[0] : Integer.MAX_VALUE)
                        .thenComparingInt(id -> position.containsKey(id) ? position.get(id)[1] : 0)
                        .thenComparing(java.util.Comparator.naturalOrder()))
                .limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public MistakesResponse mistakesOf(Long userId, String part) {
        Map<Long, Attempt> attemptOfAnswer = new HashMap<>();
        Map<Long, Answer> latest = latestAnswers(userId, attemptOfAnswer);
        List<Long> wrongIds = latest.values().stream()
                .filter(answer -> Boolean.FALSE.equals(answer.getIsCorrect()))
                .map(Answer::getQuestionId).toList();
        if (wrongIds.isEmpty()) {
            return new MistakesResponse(0, List.of(), List.of());
        }

        Map<Long, Question> questions = new HashMap<>();
        questionRepository.findAllById(wrongIds).forEach(question -> questions.put(question.getId(), question));

        Map<String, Integer> counts = new TreeMap<>();
        questions.values().forEach(question -> counts.merge(question.getPart(), 1, Integer::sum));
        List<PartCount> partCounts = counts.entrySet().stream()
                .map(entry -> new PartCount(entry.getKey(), entry.getValue())).toList();

        List<Long> shownIds = wrongIds.stream().filter(id -> {
            Question question = questions.get(id);
            return question != null && (part == null || part.isBlank() || part.equals(question.getPart()));
        }).limit(MAX_MISTAKES).toList();

        // Shared conversation / talk / passage / image of each question, from the first Test that contains it.
        Map<Long, Long> groupIdByQuestion = new HashMap<>();
        testQuestionRepository.findByQuestionIdInOrderByTestIdAscSortOrderAsc(shownIds)
                .forEach(link -> groupIdByQuestion.putIfAbsent(link.getQuestionId(), link.getGroupId()));
        Map<Long, QuestionGroup> groups = new HashMap<>();
        questionGroupRepository.findAllById(groupIdByQuestion.values().stream().filter(Objects::nonNull).distinct().toList())
                .forEach(group -> groups.put(group.getId(), group));

        List<Mistake> mistakes = new ArrayList<>();
        for (Long id : shownIds) {
            Answer answer = latest.get(id);
            Question question = questions.get(id);
            Long groupId = groupIdByQuestion.get(id);
            QuestionView view = QuestionView.from(question, groupId == null ? null : groups.get(groupId), true);
            String when = String.valueOf(attemptOfAnswer.get(id).getStartedAt());
            mistakes.add(new Mistake(answer.getAttemptId(), when, question.getPart(), view,
                    answer.getChosenOption(), answer.getExplanationText()));
        }
        return new MistakesResponse(wrongIds.size(), partCounts, mistakes);
    }
}
