package com.engcoach.attempts.dto;

import com.engcoach.attempts.Answer;

/** Response body for {@code PUT /attempts/{id}/answers/{questionId}}. */
public record AnswerResponse(Long questionId, String chosenOption) {
    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(answer.getQuestionId(), answer.getChosenOption());
    }
}
