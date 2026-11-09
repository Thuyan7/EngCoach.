package com.engcoach.attempts;

import com.engcoach.attempts.ToeicScoreEstimator.Estimate;
import com.engcoach.attempts.ToeicScoreEstimator.PartResult;
import com.engcoach.attempts.dto.ScoreResponses.AttemptScoreResponse;
import com.engcoach.attempts.dto.ScoreResponses.PartScore;
import com.engcoach.attempts.dto.ScoreResponses.PartStatsResponse;
import com.engcoach.common.AttemptAccessDeniedException;
import com.engcoach.common.AttemptNotFoundException;
import com.engcoach.parts.Question;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Điểm theo Part và điểm TOEIC quy đổi. Câu chưa trả lời tính là sai vì tổng số câu lấy từ bộ câu được giao
 * (cùng bộ câu mà phần chấm điểm dùng), không chỉ từ các câu đã lưu đáp án.
 */
@Service
public class AttemptStatsService {

    /** Chỉ Part đã có ít nhất chừng này câu mới được gọi là "yếu nhất", tránh kết luận từ một hai câu. */
    static final int MIN_QUESTIONS_FOR_WEAKEST = 5;

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final AttemptGradingService attemptGradingService;

    public AttemptStatsService(AttemptRepository attemptRepository, AnswerRepository answerRepository,
                               AttemptGradingService attemptGradingService) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.attemptGradingService = attemptGradingService;
    }

    @Transactional(readOnly = true)
    public AttemptScoreResponse scoreOfAttempt(Long userId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId).orElseThrow(AttemptNotFoundException::new);
        if (!attempt.getUserId().equals(userId)) {
            throw new AttemptAccessDeniedException();
        }
        if (!Attempt.STATUS_SUBMITTED.equals(attempt.getStatus())) {
            return new AttemptScoreResponse(List.of(), new Estimate(null, null, null));
        }
        List<PartResult> results = partResults(attempt);
        return new AttemptScoreResponse(toScores(results), ToeicScoreEstimator.estimate(results));
    }

    @Transactional(readOnly = true)
    public PartStatsResponse statsOfUser(Long userId) {
        List<Attempt> attempts = attemptRepository
                .findByUserIdAndStatusOrderByStartedAtDesc(userId, Attempt.STATUS_SUBMITTED);
        Map<String, int[]> sums = new TreeMap<>();   // Part -> {đúng, tổng}
        Estimate latest = null;
        for (Attempt attempt : attempts) {
            List<PartResult> results = partResults(attempt);
            for (PartResult result : results) {
                int[] sum = sums.computeIfAbsent(result.part(), key -> new int[2]);
                sum[0] += result.correct();
                sum[1] += result.total();
            }
            if (latest == null) {
                Estimate estimate = ToeicScoreEstimator.estimate(results);
                if (estimate.total() != null) {
                    latest = estimate;     // bài mới nhất có đủ cả Listening và Reading
                }
            }
        }
        List<PartResult> all = new ArrayList<>();
        sums.forEach((part, sum) -> all.add(new PartResult(part, sum[0], sum[1])));
        List<PartScore> parts = toScores(all);
        return new PartStatsResponse(attempts.size(), parts, weakest(parts), latest);
    }

    /** Điểm theo từng Part của một bài đã nộp, theo thứ tự Part. */
    List<PartResult> partResults(Attempt attempt) {
        List<Question> assigned = attemptGradingService.assignedQuestions(attempt);
        Set<Long> correctIds = new HashSet<>();
        for (Answer answer : answerRepository.findByAttemptId(attempt.getId())) {
            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                correctIds.add(answer.getQuestionId());
            }
        }
        Map<String, int[]> byPart = new TreeMap<>();
        for (Question question : assigned) {
            int[] sum = byPart.computeIfAbsent(question.getPart(), key -> new int[2]);
            sum[1]++;
            if (correctIds.contains(question.getId())) {
                sum[0]++;
            }
        }
        List<PartResult> results = new ArrayList<>();
        byPart.forEach((part, sum) -> results.add(new PartResult(part, sum[0], sum[1])));
        return results;
    }

    private static List<PartScore> toScores(List<PartResult> results) {
        return results.stream().filter(r -> r.total() > 0)
                .map(r -> PartScore.of(r.part(), r.correct(), r.total())).toList();
    }

    static String weakest(List<PartScore> parts) {
        PartScore weakest = null;
        for (PartScore part : parts) {
            if (part.total() < MIN_QUESTIONS_FOR_WEAKEST) continue;
            if (weakest == null || part.accuracyPercent() < weakest.accuracyPercent()) {
                weakest = part;
            }
        }
        return weakest == null ? null : weakest.part();
    }
}
