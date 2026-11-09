package com.engcoach.attempts;

import java.util.Collection;
import java.util.List;

/**
 * Ước lượng điểm TOEIC Listening and Reading (10 đến 990) từ số câu đúng.
 *
 * ETS không công bố bảng quy đổi chính thức và bảng này thay đổi theo từng đề, nên đây chỉ là ước lượng tuyến tính:
 * mỗi phần (Listening gồm Part 1-4, Reading gồm Part 5-7) quy từ tỷ lệ đúng về thang 5 đến 495, làm tròn đến bội của 5.
 * Một phần chỉ có điểm khi bài làm chứa đủ các Part của phần đó; điểm tổng chỉ có khi có cả hai phần.
 */
public final class ToeicScoreEstimator {

    public static final List<String> LISTENING_PARTS = List.of("Part 1", "Part 2", "Part 3", "Part 4");
    public static final List<String> READING_PARTS = List.of("Part 5", "Part 6", "Part 7");
    static final int MIN_SECTION_SCORE = 5;
    static final int MAX_SECTION_SCORE = 495;

    private ToeicScoreEstimator() {
    }

    /** Số câu đúng và tổng số câu của một Part trong một bài làm. */
    public record PartResult(String part, int correct, int total) {
    }

    /** Điểm quy đổi; trường nào không ước lượng được thì là null. */
    public record Estimate(Integer listening, Integer reading, Integer total) {
        public boolean isEmpty() {
            return listening == null && reading == null;
        }
    }

    public static Estimate estimate(Collection<PartResult> results) {
        Integer listening = sectionScore(results, LISTENING_PARTS);
        Integer reading = sectionScore(results, READING_PARTS);
        Integer total = listening != null && reading != null ? listening + reading : null;
        return new Estimate(listening, reading, total);
    }

    static Integer sectionScore(Collection<PartResult> results, List<String> sectionParts) {
        int correct = 0;
        int total = 0;
        for (String part : sectionParts) {
            PartResult found = results.stream().filter(r -> part.equals(r.part()) && r.total() > 0)
                    .findFirst().orElse(null);
            if (found == null) {
                return null;   // phần này chưa đủ Part nên không ước lượng
            }
            correct += found.correct();
            total += found.total();
        }
        return scale(correct, total);
    }

    static int scale(int correct, int total) {
        double raw = MIN_SECTION_SCORE + (MAX_SECTION_SCORE - MIN_SECTION_SCORE) * (double) correct / total;
        long rounded = Math.round(raw / 5.0) * 5;
        return (int) Math.max(MIN_SECTION_SCORE, Math.min(MAX_SECTION_SCORE, rounded));
    }
}
