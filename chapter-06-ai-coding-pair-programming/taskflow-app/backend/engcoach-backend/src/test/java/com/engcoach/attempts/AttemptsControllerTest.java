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
}
