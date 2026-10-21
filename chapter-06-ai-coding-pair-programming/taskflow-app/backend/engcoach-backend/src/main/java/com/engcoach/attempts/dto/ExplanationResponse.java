package com.engcoach.attempts.dto;

import com.engcoach.attempts.Answer;

/**
 * Response body for {@code POST
 * /attempts/{id}/answers/{questionId}/retry-explanation} (S8b, approved
 * design decision #5). Returns the one answer's refreshed explanation
 * state after the retry attempt — {@code explanationStatus} may still be
 * {@code UNAVAILABLE} if the retried Anthropic call failed again.
 */
public record ExplanationResponse(Long questionId, String explanationStatus, String explanationText) {

    public static ExplanationResponse from(Answer answer) {
        return new ExplanationResponse(answer.getQuestionId(), answer.getExplanationStatus(),
                answer.getExplanationText());
    }
}
