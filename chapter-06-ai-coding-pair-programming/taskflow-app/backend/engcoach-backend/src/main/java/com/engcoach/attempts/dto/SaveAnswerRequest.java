package com.engcoach.attempts.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /attempts/{id}/answers/{questionId}} —
 * approved S6a design decision #2 (this endpoint previously had no
 * documented requestBody in openapi.yaml).
 */
public class SaveAnswerRequest {

    @NotBlank
    private String chosenOption;

    public SaveAnswerRequest() {
        // required for JSON deserialization
    }

    public SaveAnswerRequest(String chosenOption) {
        this.chosenOption = chosenOption;
    }

    public String getChosenOption() {
        return chosenOption;
    }

    public void setChosenOption(String chosenOption) {
        this.chosenOption = chosenOption;
    }
}
