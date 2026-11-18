package com.engcoach.streak;

import com.engcoach.attempts.Answer;
import com.engcoach.attempts.AnswerRepository;
import com.engcoach.attempts.Attempt;
import com.engcoach.attempts.AttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The days a learner answered questions, their study streak, and the study days of one month. */
@Service
public class StreakService {

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;

    public StreakService(AttemptRepository attemptRepository, AnswerRepository answerRepository) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    /**
     * @param timeZone IANA name such as Asia/Saigon; unknown or missing falls back to UTC
     * @param month    yyyy-MM to list the study days of; unknown or missing means the current month
     */
    @Transactional(readOnly = true)
    public StreakResponse streakOf(Long userId, String timeZone, String month) {
        ZoneId zone = zoneOf(timeZone);
        LocalDate today = LocalDate.now(zone);
        YearMonth shown = monthOf(month, today);

        Set<LocalDate> studyDays = new HashSet<>();
        for (Attempt attempt : attemptRepository.findByUserId(userId)) {
            for (Answer answer : answerRepository.findByAttemptId(attempt.getId())) {
                // Answers saved before answeredAt existed fall back to the day the attempt started.
                Instant when = answer.getAnsweredAt() != null ? answer.getAnsweredAt() : attempt.getStartedAt();
                studyDays.add(when.atZone(zone).toLocalDate());
            }
        }

        StudyStreak.Result streak = StudyStreak.compute(studyDays, today);
        List<String> daysOfMonth = studyDays.stream()
                .filter(day -> YearMonth.from(day).equals(shown))
                .sorted().map(LocalDate::toString).toList();
        return new StreakResponse(streak.current(), streak.longest(), studyDays.contains(today),
                today.toString(), shown.toString(), daysOfMonth);
    }

    private ZoneId zoneOf(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) return ZoneId.of("UTC");
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }

    private YearMonth monthOf(String month, LocalDate today) {
        if (month == null || month.isBlank()) return YearMonth.from(today);
        try {
            return YearMonth.parse(month);
        } catch (DateTimeException e) {
            return YearMonth.from(today);
        }
    }
}
