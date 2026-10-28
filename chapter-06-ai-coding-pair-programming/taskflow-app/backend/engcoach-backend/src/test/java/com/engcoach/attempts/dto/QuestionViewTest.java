package com.engcoach.attempts.dto;

import com.engcoach.parts.Question;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionViewTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsPassageAndMediaWithoutExposingAnswerKey() throws Exception {
        String options = objectMapper.writeValueAsString(List.of("Room A", "Room B", "Room C", "Room D"));
        Question question = new Question(
                "Part 7",
                "Why was the meeting moved?",
                options,
                "Room B",
                "The meeting will take place in Room B.",
                "https://example.com/meeting.png",
                "https://example.com/meeting.mp3"
        );

        QuestionView view = QuestionView.from(question);

        assertEquals(question.getPrompt(), view.prompt());
        assertEquals(question.getPassage(), view.passage());
        assertEquals(question.getImageUrl(), view.imageUrl());
        assertEquals(question.getAudioUrl(), view.audioUrl());
        assertEquals(4, view.options().size());

        JsonNode response = objectMapper.valueToTree(view);
        assertTrue(response.has("options"));
        assertTrue(response.has("passage"));
        assertTrue(response.has("imageUrl"));
        assertTrue(response.has("audioUrl"));
        assertFalse(response.has("correctOption"),
                "The answer key must not be included in the question response.");
        assertFalse(response.has("correct_option"),
                "The answer key must not be included in the question response.");
    }

    @Test
    void rejectsMalformedOptionsWithClearError() {
        Question question = new Question("Part 5", "Choose one.", "not-json", "A");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> QuestionView.from(question));

        assertTrue(error.getMessage().contains("options that are not a JSON array of strings"));
    }
}
