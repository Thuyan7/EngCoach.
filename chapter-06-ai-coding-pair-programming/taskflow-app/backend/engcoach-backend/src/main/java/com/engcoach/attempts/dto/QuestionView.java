package com.engcoach.attempts.dto;

import com.engcoach.parts.Question;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * One question as shown to the person taking an attempt (S7b). Deliberately
 * has NO {@code correctOption}: the answer key must never leave the server
 * before grading.
 *
 * {@code options} is the {@code Question.options} JSON array (stored as a
 * raw string, see Question) parsed into a list of strings, returned as
 * stored. The client shows each string and sends the chosen string back as
 * {@code chosenOption}; S8a grades by comparing that string with
 * {@code Question.correctOption}, so correctOption must equal one of the
 * entries in options.
 */
public record QuestionView(Long id, String prompt, List<String> options,
                           String passage, String imageUrl, String audioUrl, String audioText) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static QuestionView from(Question question) {
        try {
            List<String> options = MAPPER.readValue(question.getOptions(), new TypeReference<List<String>>() { });
            return new QuestionView(question.getId(), question.getPrompt(), options,
                    question.getPassage(), question.getImageUrl(), question.getAudioUrl(), question.getAudioText());
        } catch (Exception e) {
            // A malformed options value is a data error in the question bank,
            // not something the caller can fix; name the question so it can
            // be found and corrected.
            throw new IllegalStateException(
                    "Question " + question.getId() + " has options that are not a JSON array of strings", e);
        }
    }
}
