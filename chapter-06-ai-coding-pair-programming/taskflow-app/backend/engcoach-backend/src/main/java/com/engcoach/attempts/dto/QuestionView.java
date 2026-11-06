package com.engcoach.attempts.dto;

import com.engcoach.parts.Question;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * One question as shown during an attempt. The answer key is omitted while
 * the attempt is in progress and included only after submission, so learners
 * can review every correct answer without exposing it before grading.
 *
 * {@code options} is the {@code Question.options} JSON array (stored as a
 * raw string, see Question) parsed into a list of strings, returned as
 * stored. The client shows each string and sends the chosen string back as
 * {@code chosenOption}; grading compares that string with
 * {@code Question.correctOption}.
 */
public record QuestionView(Long id, String prompt, List<String> options,
                           String passage, String imageUrl, String audioUrl,
                           @JsonInclude(JsonInclude.Include.NON_NULL) String correctOption) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static QuestionView from(Question question) {
        return from(question, false);
    }

    public static QuestionView from(Question question, boolean includeCorrectOption) {
        try {
            List<String> options = MAPPER.readValue(question.getOptions(), new TypeReference<List<String>>() { });
            return new QuestionView(question.getId(), question.getPrompt(), options,
                    question.getPassage(), question.getImageUrl(), question.getAudioUrl(),
                    includeCorrectOption ? question.getCorrectOption() : null);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Question " + question.getId() + " has options that are not a JSON array of strings", e);
        }
    }
}
