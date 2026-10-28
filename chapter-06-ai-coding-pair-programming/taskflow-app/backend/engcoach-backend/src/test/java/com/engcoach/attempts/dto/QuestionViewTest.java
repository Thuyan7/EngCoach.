package com.engcoach.attempts.dto;

import com.engcoach.parts.Question;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionViewTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsOptionsAndReadingMediaWithoutExposingCorrectOption() throws Exception {
        Question question = new Question(
                "Part 7",
                "What is the notice about?",
                "[\"An office renovation\",\"A holiday schedule\"]",
                "An office renovation",
                "NOTICE TO STAFF: The second-floor offices will be renovated.",
                "https://example.com/office.jpg",
                "https://example.com/notice.mp3");

        QuestionView view = QuestionView.from(question);
        JsonNode json = objectMapper.valueToTree(view);

        assertEquals("What is the notice about?", json.get("prompt").asText());
        assertEquals(2, json.get("options").size());
        assertEquals("An office renovation", json.get("options").get(0).asText());
        assertEquals("NOTICE TO STAFF: The second-floor offices will be renovated.",
                json.get("passage").asText());
        assertEquals("https://example.com/office.jpg", json.get("imageUrl").asText());
        assertEquals("https://example.com/notice.mp3", json.get("audioUrl").asText());
        assertFalse(json.has("correctOption"), "The answer key must not be included in the attempt payload");
        assertTrue(json.has("options"));
    }

    @Test
    void rejectsMalformedOptionsInsteadOfReturningCorruptQuestionData() {
        Question question = new Question("Part 5", "Choose the best word.", "not-json", "A");

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> QuestionView.from(question));
    }
}
