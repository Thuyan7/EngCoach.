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
    static final String PART_ONE_SOURCE_URL =
            "https://raw.githubusercontent.com/kdeppaei/toeic-question-ocean/main/question-bank-v31.js";
    private static final String RAW_SOURCE_BASE =
            "https://raw.githubusercontent.com/kdeppaei/toeic-question-ocean/main/";

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
            JsonNode generalBank = fetchBank(SOURCE_URL, false);
            JsonNode partOneBank = fetchBank(PART_ONE_SOURCE_URL, true);
            if (generalBank.isEmpty() && partOneBank.isEmpty()) {
                throw new IllegalStateException("Remote question banks were empty; no rows were imported");
            }

            List<Question> questionsToInsert = new ArrayList<>();
            Set<String> sourceKeys = new HashSet<>();
            int skipped = 0;

            for (JsonNode row : generalBank) {
                Question question = mapQuestion(row);
                if (shouldSkip(question, sourceKeys)) {
                    skipped++;
                } else {
                    questionsToInsert.add(question);
                }
            }

            for (JsonNode row : partOneBank) {
                Question question = mapPartOneQuestion(row);
                if (shouldSkip(question, sourceKeys)) {
                    skipped++;
                } else {
                    questionsToInsert.add(question);
                }
            }

            if (!questionsToInsert.isEmpty()) {
                questionRepository.saveAll(questionsToInsert);
            }
            System.out.printf("Remote TOEIC question-bank import complete: added %d, skipped %d duplicates, sources %s and %s%n",
                    questionsToInsert.size(), skipped, SOURCE_URL, PART_ONE_SOURCE_URL);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read or parse the remote TOEIC question banks", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Remote TOEIC question-bank import was interrupted", e);
        }
    }

    private JsonNode fetchBank(String sourceUrl, boolean partOne) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "text/plain")
                .header("User-Agent", "EngCoach-question-bank-importer")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Question source returned HTTP " + response.statusCode() + ": " + sourceUrl);
        }

        JsonNode bank = partOne
                ? parseNamedArray(response.body(), "const items =")
                : parseBankArray(response.body());
        if (bank.isEmpty()) {
            throw new IOException("Remote question bank was empty: " + sourceUrl);
        }
        return bank;
    }

    boolean shouldSkip(Question question, Set<String> sourceKeys) {
        // Part 1 questions intentionally share the same instruction prompt, so
        // their image URL—not the prompt—identifies a unique question.
        boolean isImageQuestion = "Part 1".equals(question.getPart())
                && question.getImageUrl() != null && !question.getImageUrl().isBlank();
        String key = isImageQuestion
                ? question.getPart() + "::image::" + question.getImageUrl()
                : duplicateKey(question.getPart(), question.getPrompt());

        if (!sourceKeys.add(key)) {
            return true;
        }
        return isImageQuestion
                ? questionRepository.existsByPartAndImageUrl(question.getPart(), question.getImageUrl())
                : questionRepository.existsByPartAndPrompt(question.getPart(), question.getPrompt());
    }

    private static String duplicateKey(String part, String prompt) {
        return part + "::" + prompt;
    }

    static JsonNode parseBankArray(String javascript) throws IOException {
        int start = javascript.indexOf('[');
        if (start < 0) {
            throw new IOException("Expected a JSON array in the upstream question-bank.js file");
        }
        return parseArrayAt(javascript, start);
    }

    /**
     * Parses a named JavaScript array declaration while respecting quoted strings
     * and nested arrays. The source bank uses JSON-compatible objects in this array.
     */
    static JsonNode parseNamedArray(String javascript, String declaration) throws IOException {
        int declarationIndex = javascript.indexOf(declaration);
        if (declarationIndex < 0) {
            throw new IOException("Could not find array declaration: " + declaration);
        }
        int start = javascript.indexOf('[', declarationIndex + declaration.length());
        if (start < 0) {
            throw new IOException("Could not find array after declaration: " + declaration);
        }
        return parseArrayAt(javascript, start);
    }

    private static JsonNode parseArrayAt(String javascript, int start) throws IOException {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < javascript.length(); i++) {
            char ch = javascript.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == 92) {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    String jsonArray = removeTrailingCommas(javascript.substring(start, i + 1));
                    JsonNode bank = new ObjectMapper().readTree(jsonArray);
                    if (!bank.isArray()) {
                        throw new IOException("Upstream question bank is not a JSON array");
                    }
                    return bank;
                }
            }
        }

        throw new IOException("Unterminated JSON array in upstream question bank");
    }

    private static String removeTrailingCommas(String json) {
        StringBuilder cleaned = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (inString) {
                cleaned.append(ch);
                if (escaped) {
                    escaped = false;
                } else if (ch == 92) {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                cleaned.append(ch);
            } else if (ch == ',') {
                int next = i + 1;
                while (next < json.length() && Character.isWhitespace(json.charAt(next))) {
                    next++;
                }
                if (next < json.length() && json.charAt(next) != ']' && json.charAt(next) != '}') {
                    cleaned.append(ch);
                }
            } else {
                cleaned.append(ch);
            }
        }
        return cleaned.toString();
    }

    Question mapQuestion(JsonNode row) throws IOException {
        String partNumber = requiredText(row, "part");
        String prompt = requiredText(row, "prompt");
        List<String> choices = readChoices(row);
        int answerIndex = readAnswerIndex(row, choices.size());

        String passage = optionalText(row, "passage");
        String part = "Part " + partNumber;
        String optionsJson = objectMapper.writeValueAsString(choices);
        // Keep transcript separate from a playable audio URL; the frontend may
        // choose speech synthesis or display a transcript during review.
        String audioText = optionalText(row, "audioText");
        String audioUrl = optionalText(row, "audioUrl");
        return new Question(part, prompt, optionsJson, choices.get(answerIndex),
                passage.isBlank() ? null : passage, null,
                audioUrl.isBlank() ? null : audioUrl,
                audioText.isBlank() ? null : audioText);
    }

    Question mapPartOneQuestion(JsonNode row) throws IOException {
        List<String> choices = readChoices(row);
        int answerIndex = readAnswerIndex(row, choices.size());
        String imagePath = requiredText(row, "image");

        // Only allow known repository-relative Part 1 assets; never accept arbitrary URLs.
        if (!imagePath.startsWith("assets/part1/") || imagePath.contains("..")
                || imagePath.indexOf((char) 92) >= 0) {
            throw new IOException("Part 1 question contains an invalid image path");
        }

        String prompt = "Look at the picture and select the sentence that best describes it.";
        String imageUrl = RAW_SOURCE_BASE + imagePath;
        String optionsJson = objectMapper.writeValueAsString(choices);
        // Part 1 is picture-based. The upstream source synthesizes audioText
        // from the answer choices, which is not an actual listening transcript.
        // Do not expose those choices as audio/transcript content.
        return new Question("Part 1", prompt, optionsJson, choices.get(answerIndex),
                null, imageUrl, null, null);
    }

    private List<String> readChoices(JsonNode row) throws IOException {
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
        return choices;
    }

    private static int readAnswerIndex(JsonNode row, int choicesCount) throws IOException {
        JsonNode answerNode = row.get("answer");
        if (answerNode == null || !answerNode.canConvertToInt()) {
            throw new IOException("Question has no numeric answer index");
        }
        int answerIndex = answerNode.asInt();
        if (answerIndex < 0 || answerIndex >= choicesCount) {
            throw new IOException("Question answer index is outside the choices array");
        }
        return answerIndex;
    }

    private static String requiredText(JsonNode row, String field) throws IOException {
        JsonNode value = row.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IOException("Question is missing required field: " + field);
        }
        return value.asText();
    }

    private static String optionalText(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
