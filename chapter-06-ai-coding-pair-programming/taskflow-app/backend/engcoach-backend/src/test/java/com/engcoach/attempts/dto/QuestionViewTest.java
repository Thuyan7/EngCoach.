package com.engcoach.attempts.dto;

import com.engcoach.parts.Question;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionViewTest {

    @Test
    void exposesQuestionContentAndMediaWithoutAnswerKey() {
        Question question = new Question(
                "Part 7",
                "What is the purpose of the notice?",
                "[\"To announce a schedule change\",\"To request payment\"]",
                "To announce a schedule change",
                "Office notice: The meeting will begin at 10 a.m.",
                "https://example.com/notice.jpg",
                "https://example.com/notice.mp3");

        QuestionView view = QuestionView.from(question);

        assertEquals("What is the purpose of the notice?", view.prompt());
        assertEquals(2, view.options().size());
        assertEquals("To announce a schedule change", view.options().get(0));
        assertEquals("Office notice: The meeting will begin at 10 a.m.", view.passage());
        assertEquals("https://example.com/notice.jpg", view.imageUrl());
        assertEquals("https://example.com/notice.mp3", view.audioUrl());
        assertFalse(java.util.Arrays.stream(QuestionView.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("correctOption")));
    }

    @Test
    void rejectsMalformedOptionsWithQuestionIdInError() {
        Question question = new Question("Part 5", "Choose the answer", "not-json", "A");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> QuestionView.from(question));

        assertEquals("Question null has options that are not a JSON array of strings", error.getMessage());
    }
}
