package com.engcoach.attempts.dto;

import java.util.List;

/** DTOs for the "review your mistakes" list. */
public final class MistakeResponses {

    private MistakeResponses() {
    }

    /** One question the learner got wrong the last time they answered it. */
    public record Mistake(Long attemptId, String answeredAt, String part, QuestionView question,
                          String chosenOption, String explanationText) {
    }

    public record PartCount(String part, int count) {
    }

    /** {@code partCounts} always covers all Parts, so the UI can show filters even when one Part is selected. */
    public record MistakesResponse(int total, List<PartCount> partCounts, List<Mistake> mistakes) {
    }
}
