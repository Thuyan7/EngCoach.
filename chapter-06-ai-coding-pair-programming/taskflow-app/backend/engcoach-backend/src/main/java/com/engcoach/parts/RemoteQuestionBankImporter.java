package com.engcoach.parts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time, opt-in import of original TOEIC-style practice questions from
 * TOEIC Question Ocean. This is a GitHub-hosted data file, not a live question API.
 *
 * The importer intentionally runs only when the local question table is empty.
 * It never overwrites a user's existing bank.
 */
@Component
@ConditionalOnProperty(prefix = "engcoach.question-bank", name = "import-remote", havingValue = "true")
public class RemoteQuestionBankImporter implements CommandLineRunner {

    static final String SOURCE_URL =
            "https://raw.githubusercontent.com/kdeppaei/toeic-question-ocean/main/question-bank.js";

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RemoteQuestionBankImporter(QuestionRepository questionRepository, ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() > 0) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(SOURCE_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "text/plain")
                    .header("User-Agent", "EngCoach-question-bank-importer")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Question source returned HTTP " + response.statusCode());
            }

            JsonNode bank = parseBankArray(response.body());
            List<Question> questions = new ArrayList<>();
            for (JsonNode row : bank) {
                questions.add(mapQuestion(row));
            }
            if (questions.isEmpty()) {
                throw new IllegalStateException("Remote question bank was empty; no rows were imported");
            }

            questionRepository.saveAll(questions);
            System.out.printf("Imported %d original TOEIC-style questions from %s%n",
                    questions.size(), SOURCE_URL);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the remote TOEIC question bank", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Remote TOEIC question-bank import was interrupted", e);
        }
    }

    static JsonNode parseBankArray(String javascript) throws IOException {
        int start = javascript.indexOf('[');
        int end = javascript.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IOException("Expected a JSON array in the upstream question-bank.js file");
        }

        JsonNode bank = new ObjectMapper().readTree(javascript.substring(start, end + 1));
        if (!bank.isArray()) {
            throw new IOException("Upstream question bank is not a JSON array");
        }
        return bank;
    }

    private Question mapQuestion(JsonNode row) throws IOException {
        String partNumber = requiredText(row, "part");
        String prompt = requiredText(row, "prompt");
        JsonNode choicesNode = row.get("choices");
        if (choicesNode == null || !choicesNode.isArray() || choicesNode.size() < 2) {
            throw new IOException("Question has no valid choices");
        }

        List<String> choices = new ArrayList<>();
        for (JsonNode choice : choicesNode) {
            if (!choice.isTextual() || choice.asText().isBlank()) {
                throw new IOException("Question contains an empty or invalid choice");
            }
            choices.add(choice.asText());
        }

        JsonNode answerNode = row.get("answer");
        if (answerNode == null || !answerNode.canConvertToInt()) {
            throw new IOException("Question has no numeric answer index");
        }
        int answerIndex = answerNode.asInt();
        if (answerIndex < 0 || answerIndex >= choices.size()) {
            throw new IOException("Question answer index is outside the choices array");
        }

        String passage = optionalText(row, "passage");
        String part = "Part " + partNumber;
        String optionsJson = objectMapper.writeValueAsString(choices);
        // The upstream file contains transcript/text fields, not direct audio file URLs.
        return new Question(part, prompt, optionsJson, choices.get(answerIndex),
                passage.isBlank() ? null : passage, null, null);
    }

    private static String requiredText(JsonNode row, String field) throws IOException {
        JsonNode value = row.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new IOException("Question is missing required field: " + field);
        }
        return value.asText();
    }

    private static String optionalText(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
