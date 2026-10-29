package com.engcoach.parts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RemoteQuestionBankImporterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesJsonArrayWrappedInJavascriptAssignment() throws IOException {
        JsonNode bank = RemoteQuestionBankImporter.parseBankArray(
                "window.BUILTIN_BANK = [{\"part\":\"5\",\"prompt\":\"Choose.\","
                        + "\"choices\":[\"A\",\"B\"],\"answer\":1}];");

        assertEquals(1, bank.size());
        assertEquals("Choose.", bank.get(0).get("prompt").asText());
        assertEquals(1, bank.get(0).get("answer").asInt());
    }

    @Test
    void parsesNamedArrayWithoutIncludingLaterArrays() throws IOException {
        JsonNode bank = RemoteQuestionBankImporter.parseNamedArray(
                "const items = [{\"image\":\"assets/part1/photo.jpg\","
                        + "\"choices\":[\"A [near]\",\"B\"],\"answer\":0}];"
                        + "window.BUILTIN_BANK.push(...items.map(x => x));");

        assertEquals(1, bank.size());
        assertEquals("assets/part1/photo.jpg", bank.get(0).get("image").asText());
        assertEquals("A [near]", bank.get(0).get("choices").get(0).asText());
    }

    @Test
    void mapsListeningTranscriptWithoutPretendingItIsAnAudioFile() throws IOException {
        RemoteQuestionBankImporter importer = new RemoteQuestionBankImporter(
                mock(QuestionRepository.class), objectMapper);
        JsonNode row = objectMapper.readTree("""
                {
                  "part": "2",
                  "prompt": "Where is the meeting?",
                  "choices": ["In the conference room.", "At noon.", "Ms. Lee."],
                  "answer": 0,
                  "audioText": "Where is the meeting? In the conference room. At noon. Ms. Lee."
                }
                """);

        Question question = importer.mapQuestion(row);

        assertEquals("Part 2", question.getPart());
        assertEquals("In the conference room.", question.getCorrectOption());
        assertEquals("Where is the meeting? In the conference room. At noon. Ms. Lee.",
                question.getAudioText());
        assertNull(question.getAudioUrl());
    }

    @Test
    void mapsPartOneQuestionAndBuildsRawImageUrl() throws IOException {
        RemoteQuestionBankImporter importer = new RemoteQuestionBankImporter(
                mock(QuestionRepository.class), objectMapper);
        JsonNode row = objectMapper.readTree("""
                {
                  "image": "assets/part1/warehouse.jpg",
                  "choices": ["Workers are sitting.", "Workers are standing."],
                  "answer": 1
                }
                """);

        Question question = importer.mapPartOneQuestion(row);

        assertEquals("Part 1", question.getPart());
        assertEquals("Look at the picture and select the sentence that best describes it.",
                question.getPrompt());
        assertEquals("Workers are standing.", question.getCorrectOption());
        assertEquals("https://raw.githubusercontent.com/kdeppaei/toeic-question-ocean/main/assets/part1/warehouse.jpg",
                question.getImageUrl());
        assertNull(question.getAudioUrl());
        assertEquals("A. Workers are sitting. B. Workers are standing.", question.getAudioText());
    }

    @Test
    void deduplicatesPartOneByImageInsteadOfSharedPrompt() {
        QuestionRepository repository = mock(QuestionRepository.class);
        RemoteQuestionBankImporter importer = new RemoteQuestionBankImporter(repository, objectMapper);
        Set<String> sourceKeys = new HashSet<>();
        Question first = new Question("Part 1", "Look at the picture.", "[\"A\",\"B\"]", "A",
                null, "https://example.com/one.jpg", null);
        Question second = new Question("Part 1", "Look at the picture.", "[\"A\",\"B\"]", "B",
                null, "https://example.com/two.jpg", null);

        assertFalse(importer.shouldSkip(first, sourceKeys));
        assertFalse(importer.shouldSkip(second, sourceKeys));
        assertTrue(importer.shouldSkip(first, sourceKeys));
    }

    @Test
    void rejectsUnsafePartOneImagePaths() throws IOException {
        RemoteQuestionBankImporter importer = new RemoteQuestionBankImporter(
                mock(QuestionRepository.class), objectMapper);
        JsonNode row = objectMapper.readTree("""
                {
                  "image": "https://example.com/image.jpg",
                  "choices": ["A", "B"],
                  "answer": 0
                }
                """);

        assertThrows(IOException.class, () -> importer.mapPartOneQuestion(row));
    }

    @Test
    void rejectsJavascriptWithoutAJsonArray() {
        assertThrows(IOException.class,
                () -> RemoteQuestionBankImporter.parseBankArray("window.BUILTIN_BANK = undefined;"));
    }

    @Test
    void parsesAnEmptyArrayButImporterRejectsItBeforeSaving() throws IOException {
        JsonNode bank = RemoteQuestionBankImporter.parseBankArray("window.BUILTIN_BANK = [];");
        assertEquals(0, bank.size());
    }
}
