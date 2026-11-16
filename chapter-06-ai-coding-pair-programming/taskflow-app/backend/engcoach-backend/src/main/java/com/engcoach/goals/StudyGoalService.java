package com.engcoach.goals;

import com.engcoach.attempts.Answer;
import com.engcoach.attempts.AnswerRepository;
import com.engcoach.attempts.Attempt;
import com.engcoach.attempts.AttemptRepository;
import com.engcoach.attempts.AttemptStatsService;
import com.engcoach.attempts.ToeicScoreEstimator;
import com.engcoach.goals.GoalsDtos.GoalsResponse;
import com.engcoach.goals.GoalsDtos.UpdateGoalsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

/** Study goals, today's progress and the study streak of one learner. */
@Service
public class StudyGoalService {

    private final UserGoalRepository goalRepository;
    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final AttemptStatsService attemptStatsService;

    public StudyGoalService(UserGoalRepository goalRepository, AttemptRepository attemptRepository,
                            AnswerRepository answerRepository, AttemptStatsService attemptStatsService) {
        this.goalRepository = goalRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.attemptStatsService = attemptStatsService;
    }

    @Transactional(readOnly = true)
    public GoalsResponse goalsOf(Long userId, String timeZone) {
        return build(userId, goalRepository.findByUserId(userId).orElse(null), zoneOf(timeZone));
    }

    @Transactional
    public GoalsResponse update(Long userId, UpdateGoalsRequest request, String timeZone) {
        UserGoal goal = goalRepository.findByUserId(userId).orElse(null);
        if (goal == null) {
            goal = new UserGoal(userId, request.targetScore(), request.dailyQuestionGoal());
        } else {
            goal.update(request.targetScore(), request.dailyQuestionGoal());
        }
        return build(userId, goalRepository.save(goal), zoneOf(timeZone));
    }

    private GoalsResponse build(Long userId, UserGoal goal, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int answeredToday = 0;
        Set<LocalDate> studyDays = new HashSet<>();
        for (Attempt attempt : attemptRepository.findByUserId(userId)) {
            for (Answer answer : answerRepository.findByAttemptId(attempt.getId())) {
                // Answers saved before answeredAt existed fall back to the day the attempt started.
                Instant when = answer.getAnsweredAt() != null ? answer.getAnsweredAt() : attempt.getStartedAt();
                LocalDate day = when.atZone(zone).toLocalDate();
                studyDays.add(day);
                if (day.equals(today)) answeredToday++;
            }
        }
        StudyStreak.Result streak = StudyStreak.compute(studyDays, today);

        int dailyGoal = goal == null ? UserGoal.DEFAULT_DAILY_QUESTION_GOAL : goal.getDailyQuestionGoal();
        ToeicScoreEstimator.Estimate latest = attemptStatsService.statsOfUser(userId).latestEstimate();
        Integer estimated = latest == null ? null : latest.total();
        return new GoalsResponse(goal == null ? null : goal.getTargetScore(), dailyGoal, answeredToday,
                answeredToday >= dailyGoal, streak.current(), streak.longest(), estimated);
    }

    /** The learner's own time zone decides which calendar day an answer belongs to; unknown zones fall back to UTC. */
    private ZoneId zoneOf(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) return ZoneId.of("UTC");
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
