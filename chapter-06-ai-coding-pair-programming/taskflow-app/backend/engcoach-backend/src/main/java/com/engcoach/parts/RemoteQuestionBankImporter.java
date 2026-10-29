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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Opt-in import of original TOEIC-style practice questions from TOEIC Question Ocean.
 * This is a GitHub-hosted data file, not a live question API.
 *
 * Existing questions are preserved. Rows matching an existing (part, prompt)
 * pair are skipped, making repeat imports safe for the same source bank.
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
            if (bank.isEmpty()) {
                throw new IllegalStateException("Remote question bank was empty; no rows were imported");
            }

            List<Question> questionsToInsert = new ArrayList<>();
            Set<String> sourceKeys = new HashSet<>();
            int skipped = 0;
            for (JsonNode row : bank) {
                Question question = mapQuestion(row);
                String key = duplicateKey(question.getPart(), question.getPrompt());
                if (!sourceKeys.add(key) || questionRepository.existsByPartAndPrompt(
                        question.getPart(), question.getPrompt())) {
                    skipped++;
                    continue;
                }
                questionsToInsert.add(question);
            }

            if (!questionsToInsert.isEmpty()) {
                questionRepository.saveAll(questionsToInsert);
            }
            System.out.printf("Remote TOEIC question-bank import complete: added %d, skipped %d duplicates, source %s%n",
                    questionsToInsert.size(), skipped, SOURCE_URL);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the remote TOEIC question bank", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Remote TOEIC question-bank import was interrupted", e);
        }
    }

    private static String duplicateKey(String part, String prompt) {
        return part + "\u0000" + prompt;
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
