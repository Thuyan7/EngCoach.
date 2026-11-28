package com.engcoach.attempts;

import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * P7.3 / finding R2: submitting must not hang on a slow AI service. A single
 * call is cut off by a timeout, and the whole submit has a time budget; what
 * is not explained in time is reported as UNAVAILABLE (retry endpoint exists).
 * A local fake "Anthropic" server stands in for the real API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExplanationTimeoutTest {

    private static final HttpServer FAKE_AI = startFakeAi();
    private static final AtomicInteger HITS = new AtomicInteger();
    private static volatile long delayMillis = 0;

    private static HttpServer startFakeAi() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/v1/messages", exchange -> {
                HITS.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                byte[] body = "{\"content\":[{\"type\":\"text\",\"text\":\"Because.\"}]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                try {
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                } catch (java.io.IOException clientGaveUp) {
                    // the caller timed out and closed the connection
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void aiProperties(DynamicPropertyRegistry registry) {
        registry.add("engcoach.anthropic.api-key", () -> "test-key");
        registry.add("engcoach.anthropic.base-url", () -> "http://127.0.0.1:" + FAKE_AI.getAddress().getPort());
        registry.add("engcoach.anthropic.timeout", () -> "1s");
        registry.add("engcoach.anthropic.submit-budget", () -> "1s");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void reset() {
        questionRepository.deleteAll();
        HITS.set(0);
        delayMillis = 0;
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-battery"));
        String response = mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    /** Starts a Part 5 attempt with {@code wrongAnswers} wrong answers and submits it. */
    private JsonNode submitWithWrongAnswers(String email, int wrongAnswers) throws Exception {
        String token = registerAndGetToken(email);
        long[] questionIds = new long[wrongAnswers];
        for (int i = 0; i < wrongAnswers; i++) {
            questionIds[i] = questionRepository.save(new Question("Part 5", "Prompt " + i,
                    "[\"A\",\"B\",\"C\",\"D\"]", "A")).getId();
        }
        String start = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"))))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(start).get("id").asLong();
        for (long questionId : questionIds) {
            mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                    .header("Authorization", "Bearer " + token).contentType("application/json")
                    .content(objectMapper.writeValueAsString(Map.of("chosenOption", "B"))));
        }
        String submitted = mockMvc.perform(post("/attempts/" + attemptId + "/submit")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(submitted);
    }

    private static long countStatus(JsonNode submitted, String status) {
        long count = 0;
        for (JsonNode answer : submitted.get("answers")) {
            if (status.equals(answer.get("explanationStatus").asText())) {
                count++;
            }
        }
        return count;
    }

    @Test
    void slowAiCall_isCutOffAtTheTimeout_andTheAnswerIsUnavailable() throws Exception {
        delayMillis = 4000;

        long started = System.nanoTime();
        JsonNode submitted = submitWithWrongAnswers("r2-timeout@example.com", 1);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertEquals(1, HITS.get(), "the call must go to the configured AI endpoint");
        assertEquals(1, countStatus(submitted, "UNAVAILABLE"));
        assertTrue(elapsedMillis < 3500, "submit took " + elapsedMillis + " ms, expected about the 1 s timeout");
    }

    @Test
    void whenTheSubmitBudgetIsSpent_remainingAnswersAreUnavailableWithoutCallingTheAi() throws Exception {
        delayMillis = 700;

        JsonNode submitted = submitWithWrongAnswers("r2-budget@example.com", 3);

        assertEquals(2, HITS.get(), "the third call starts after the 1 s budget and must be skipped");
        assertEquals(2, countStatus(submitted, "OK"));
        assertEquals(1, countStatus(submitted, "UNAVAILABLE"));
    }
}
