package com.engcoach.attempts.dto;

import com.engcoach.attempts.ToeicScoreEstimator;

import java.util.List;

/** Các DTO cho điểm quy đổi và thống kê theo Part. */
public final class ScoreResponses {

    private ScoreResponses() {
    }

    public record PartScore(String part, int correct, int total, int accuracyPercent) {
        public static PartScore of(String part, int correct, int total) {
            return new PartScore(part, correct, total, total == 0 ? 0 : Math.round(100f * correct / total));
        }
    }

    /** Điểm theo Part và điểm TOEIC quy đổi của một bài đã nộp. */
    public record AttemptScoreResponse(List<PartScore> parts, ToeicScoreEstimator.Estimate estimate) {
    }

    /** Tổng hợp mọi bài đã nộp của người dùng: tỷ lệ đúng theo Part, Part yếu nhất, điểm quy đổi gần nhất. */
    public record PartStatsResponse(int attemptsCounted, List<PartScore> parts, String weakestPart,
                                    ToeicScoreEstimator.Estimate latestEstimate) {
    }
}
