package com.engcoach.attempts;

import com.engcoach.attempts.ToeicScoreEstimator.Estimate;
import com.engcoach.attempts.ToeicScoreEstimator.PartResult;
import com.engcoach.attempts.dto.ScoreResponses.PartScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToeicScoreEstimatorTest {

    private static List<PartResult> fullTest(int listeningCorrect, int readingCorrect) {
        // Đề chuẩn: Listening 6 + 25 + 39 + 30 = 100 câu, Reading 30 + 16 + 54 = 100 câu.
        return List.of(
                new PartResult("Part 1", listeningCorrect == 100 ? 6 : 0, 6),
                new PartResult("Part 2", listeningCorrect == 100 ? 25 : 0, 25),
                new PartResult("Part 3", listeningCorrect == 100 ? 39 : listeningCorrect, 39),
                new PartResult("Part 4", listeningCorrect == 100 ? 30 : 0, 30),
                new PartResult("Part 5", readingCorrect == 100 ? 30 : 0, 30),
                new PartResult("Part 6", readingCorrect == 100 ? 16 : 0, 16),
                new PartResult("Part 7", readingCorrect == 100 ? 54 : readingCorrect, 54));
    }

    @Test
    void scaleStaysBetween5And495AndRoundsToMultiplesOfFive() {
        assertEquals(5, ToeicScoreEstimator.scale(0, 100));
        assertEquals(495, ToeicScoreEstimator.scale(100, 100));
        assertEquals(250, ToeicScoreEstimator.scale(50, 100));
        assertEquals(190, ToeicScoreEstimator.scale(3, 8));
        for (int correct = 0; correct <= 100; correct++) {
            int score = ToeicScoreEstimator.scale(correct, 100);
            assertTrue(score >= 5 && score <= 495 && score % 5 == 0, "correct=" + correct + " -> " + score);
        }
    }

    @Test
    void perfectAndEmptyFullTests() {
        Estimate perfect = ToeicScoreEstimator.estimate(fullTest(100, 100));
        assertEquals(495, perfect.listening());
        assertEquals(495, perfect.reading());
        assertEquals(990, perfect.total());
        Estimate empty = ToeicScoreEstimator.estimate(fullTest(0, 0));
        assertEquals(10, empty.total());
    }

    @Test
    void onlyOneSectionGivesThatSectionAndNoTotal() {
        List<PartResult> listeningOnly = List.of(
                new PartResult("Part 1", 3, 6), new PartResult("Part 2", 20, 25),
                new PartResult("Part 3", 30, 39), new PartResult("Part 4", 20, 30));
        Estimate estimate = ToeicScoreEstimator.estimate(listeningOnly);
        assertEquals(ToeicScoreEstimator.scale(73, 100), estimate.listening());
        assertNull(estimate.reading());
        assertNull(estimate.total());
    }

    @Test
    void partialSectionOrSinglePartGivesNoEstimate() {
        assertTrue(ToeicScoreEstimator.estimate(List.of(new PartResult("Part 5", 20, 30))).isEmpty());
        List<PartResult> missingPart4 = List.of(new PartResult("Part 1", 3, 6), new PartResult("Part 2", 20, 25),
                new PartResult("Part 3", 30, 39));
        assertNull(ToeicScoreEstimator.estimate(missingPart4).listening());
        assertTrue(ToeicScoreEstimator.estimate(List.of()).isEmpty());
    }

    @Test
    void weakestPartIgnoresPartsWithTooFewQuestions() {
        List<PartScore> parts = List.of(PartScore.of("Part 1", 0, 2), PartScore.of("Part 2", 20, 25),
                PartScore.of("Part 5", 12, 30), PartScore.of("Part 7", 40, 54));
        assertEquals("Part 5", AttemptStatsService.weakest(parts));
        assertNull(AttemptStatsService.weakest(List.of(PartScore.of("Part 1", 0, 2))));
        assertNull(AttemptStatsService.weakest(List.of()));
    }
}
