package com.engcoach.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * S8b / ADR-004: the single, server-side-only integration point with the
 * Anthropic API, used exclusively to generate a short explanation for one
 * wrong TOEIC answer. The client never calls this directly and never sees
 * the API key (ADR-004's whole point) — only {@code com.engcoach.attempts}
 * calls this class.
 *
 * Approved S8b design decision #4: any failure — missing/blank API key,
 * network error, timeout, non-2xx response, an empty or malformed
 * response body — is caught here and reported back as
 * {@link Optional#empty()}. Nothing this class does ever throws past its
 * own boundary; the caller (AttemptsService) decides what an empty result
 * means (explanationStatus = UNAVAILABLE) without needing to know why it
 * failed. Decision #7: a blank {@code api-key} (unset
 * {@code ANTHROPIC_API_KEY}, e.g. this dev sandbox) short-circuits to
 * {@link Optional#empty()} without attempting a network call at all.
 */
@Service
public class AnthropicExplanationService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicExplanationService.class);
    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 300;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AnthropicExplanationService(@Value("${engcoach.anthropic.api-key:}") String apiKey,
                                        @Value("${engcoach.anthropic.model:claude-3-5-haiku-latest}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    /**
     * Approved S8b design decision #6: the prompt sent to Anthropic is the
     * question's {@code prompt}, its raw {@code options} JSON,
     * {@code correctOption}, and the user's {@code chosenOption} — enough
     * for the model to explain why the chosen option is wrong and the
     * correct one is right, in Vietnamese (matching this project's
     * documentation language), without needing any other context.
     */
    public Optional<String> generateExplanation(String questionPrompt, String optionsJson,
                                                  String correctOption, String chosenOption) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String userMessage = buildUserMessage(questionPrompt, optionsJson, correctOption, chosenOption);
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", MAX_TOKENS,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            AnthropicMessageResponse response = restClient.post()
                    .uri(MESSAGES_PATH)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(AnthropicMessageResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                log.warn("Anthropic explanation call returned no content");
                return Optional.empty();
            }

            String text = response.content().get(0).text();
            return (text == null || text.isBlank()) ? Optional.empty() : Optional.of(text.trim());
        } catch (Exception ex) {
            // Any failure — timeout, connection error, non-2xx, malformed
            // body — degrades to UNAVAILABLE rather than breaking the
            // caller's request (ADR-004's consequence, approved decision
            // #4). Logged, not rethrown.
            log.warn("Anthropic explanation call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildUserMessage(String questionPrompt, String optionsJson,
                                     String correctOption, String chosenOption) {
        return """
                Bạn là gia sư TOEIC. Một học viên đã trả lời sai một câu hỏi trắc nghiệm.
                Hãy giải thích ngắn gọn (2-4 câu, bằng tiếng Việt) vì sao đáp án đúng là đúng
                và vì sao lựa chọn của học viên là sai. Không lặp lại nguyên văn câu hỏi.

                Câu hỏi: %s
                Các lựa chọn (JSON): %s
                Đáp án đúng: %s
                Học viên đã chọn: %s
                """.formatted(questionPrompt, optionsJson, correctOption, chosenOption);
    }

    private record AnthropicMessageResponse(List<ContentBlock> content) {
    }

    private record ContentBlock(String type, String text) {
    }
}
