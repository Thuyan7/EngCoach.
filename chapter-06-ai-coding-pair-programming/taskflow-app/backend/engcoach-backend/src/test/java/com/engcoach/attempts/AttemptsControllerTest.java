package com.engcoach.attempts;

import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice S6a. implementation-plan.md's definition of done: "Starting a Part
 * attempt persists an Attempt row; saving an answer persists/updates the
 * matching Answer row." Covers the 5 approved S6a design decisions (see
 * SLICE-S6A-NOTES.md): resume-by-(user,type,part), the new
 * {@code chosenOption} requestBody, the 422 empty-Part rejection, the 403
 * ownership check, and the plain-string "IN_PROGRESS" status.
 *
 * Also covers Slice S7a (same {@code AttemptsController}, same test
 * class): {@code GET /attempts/{id}} returning saved answers and
 * server-computed remaining time. See SLICE-S7A-NOTES.md for S7a's
 * approved design decisions (not status-gated, null
 * {@code remainingSeconds} for untimed attempts, 404-then-403 ordering).
 *
 * Same pattern as PartsControllerTest: clears the questions table before
 * every test (Spring caches the ApplicationContext/H2 instance across test
 * classes in one Maven run) and seeds Question rows directly via the
 * repository, since no question-authoring mechanism exists yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttemptsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearQuestions() {
        questionRepository.deleteAll();
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "correct-horse-battery"
        ));
        String response = mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private Long seedQuestion(String part) {
        Question question = questionRepository.save(new Question(part, "Prompt text.",
                "[\"A\",\"B\",\"C\",\"D\"]", "A"));
        return question.getId();
    }

    @Test
    void startAttempt_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));

        mockMvc.perform(post("/attempts").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startAttempt_partPracticeWithNoQuestionsForPart_returns422() throws Exception {
        String token = registerAndGetToken("s6a-empty-part@example.com");
        String body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 7"));

        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void startAttempt_partPracticeWithQuestions_createsInProgressAttempt() throws Exception {
        String token = registerAndGetToken("s6a-start@example.com");
        seedQuestion("Part 5");
        String body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));

        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("PART_PRACTICE"))
                .andExpect(jsonPath("$.part").value("Part 5"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void startAttempt_calledTwiceForSameUserTypePart_resumesSameAttempt() throws Exception {
        String token = registerAndGetToken("s6a-resume@example.com");
        seedQuestion("Part 5");
        String body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));

        String firstResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(firstResponse).get("id").asLong();

        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstId));
    }

    @Test
    void saveAnswer_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("chosenOption", "A"));

        mockMvc.perform(put("/attempts/1/answers/1").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saveAnswer_onNonexistentAttempt_returns404() throws Exception {
        String token = registerAndGetToken("s6a-answer-404@example.com");
        String body = objectMapper.writeValueAsString(Map.of("chosenOption", "A"));

        mockMvc.perform(put("/attempts/999999/answers/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void saveAnswer_onOwnAttempt_persistsAndReturns200() throws Exception {
        String token = registerAndGetToken("s6a-answer-own@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        String answerBody = objectMapper.writeValueAsString(Map.of("chosenOption", "B"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(answerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(questionId))
                .andExpect(jsonPath("$.chosenOption").value("B"));

        // Re-answering the same question overwrites, rather than creating a
        // second Answer row for the same (attempt, question) pair.
        String overwriteBody = objectMapper.writeValueAsString(Map.of("chosenOption", "C"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(overwriteBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chosenOption").value("C"));
    }

    @Test
    void saveAnswer_onAnotherUsersAttempt_returns403() throws Exception {
        String ownerToken = registerAndGetToken("s6a-owner@example.com");
        String intruderToken = registerAndGetToken("s6a-intruder@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        String answerBody = objectMapper.writeValueAsString(Map.of("chosenOption", "A"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType("application/json").content(answerBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAttempt_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/attempts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAttempt_onNonexistentAttempt_returns404() throws Exception {
        String token = registerAndGetToken("s7a-get-404@example.com");

        mockMvc.perform(get("/attempts/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAttempt_onAnotherUsersAttempt_returns403() throws Exception {
        String ownerToken = registerAndGetToken("s7a-owner@example.com");
        String intruderToken = registerAndGetToken("s7a-intruder@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAttempt_onOwnPartPracticeAttempt_returnsSavedAnswersAndNullRemainingSeconds() throws Exception {
        String token = registerAndGetToken("s7a-get-own@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        String answerBody = objectMapper.writeValueAsString(Map.of("chosenOption", "B"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(answerBody))
                .andExpect(status().isOk());

        // PART_PRACTICE is untimed: no Attempt row anywhere in this slice
        // ever sets timeLimitSeconds, so remainingSeconds must be null
        // ("untimed"), never 0 ("out of time") — approved S7a decision #2.
        // Jackson serializes a null record field as `"remainingSeconds":
        // null` (no @JsonInclude(NON_NULL) configured anywhere in this
        // project), so the key exists with a null value — asserted with
        // Hamcrest's nullValue(), not doesNotExist().
        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attemptId))
                .andExpect(jsonPath("$.type").value("PART_PRACTICE"))
                .andExpect(jsonPath("$.part").value("Part 5"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.remainingSeconds").value(nullValue()))
                .andExpect(jsonPath("$.answers.length()").value(1))
                .andExpect(jsonPath("$.answers[0].questionId").value(questionId))
                .andExpect(jsonPath("$.answers[0].chosenOption").value("B"));
    }

    @Test
    void getAttempt_withNoSavedAnswersYet_returnsEmptyAnswersArray() throws Exception {
        String token = registerAndGetToken("s7a-get-empty@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers").isArray())
                .andExpect(jsonPath("$.answers").isEmpty());
    }
}
