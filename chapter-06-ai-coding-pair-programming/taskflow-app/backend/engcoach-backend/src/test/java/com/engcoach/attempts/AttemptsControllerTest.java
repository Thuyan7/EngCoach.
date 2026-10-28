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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Also covers Slice S8a: {@code POST /attempts/{id}/submit} grading. See
 * SLICE-S8A-NOTES.md for S8a's approved design decisions (raw-count
 * score plus persisted totalQuestions, unanswered-as-incorrect,
 * idempotent resubmission). Every {@link #seedQuestion} in this class
 * uses {@code correctOption = "A"}, so tests control correctness simply
 * by choosing whether to answer "A" (correct) or anything else
 * (incorrect) or not at all (unanswered, per decision #2).
 *
 * Also covers Slice S8b: AI-generated explanations and the retry
 * endpoint. See SLICE-S8B-NOTES.md for S8b's approved design decisions.
 * No {@code ANTHROPIC_API_KEY} is set anywhere under {@code src/test},
 * so every explanation call in this test class deterministically
 * resolves to {@code UNAVAILABLE} (approved decision #7) — these tests
 * assert that fallback path, not a real Anthropic response.
 *
 * Also covers Slice S10a: {@code GET /attempts} (history) and
 * {@code DELETE /attempts/{id}}. See SLICE-S10A-NOTES.md for S10a's
 * approved design decisions (SUBMITTED-only history, most-recent-first
 * by startedAt, delete not status-gated, explicit Answer cascade).
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
    void saveAnswer_optionNotPresentInQuestion_returns422() throws Exception {
        String token = registerAndGetToken("s6a-invalid-option@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        String body = objectMapper.writeValueAsString(Map.of("chosenOption", "not-an-option"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void saveAnswer_questionFromDifferentPart_returns422() throws Exception {
        String token = registerAndGetToken("s6a-wrong-part@example.com");
        seedQuestion("Part 5");
        Long part7QuestionId = seedQuestion("Part 7");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        String body = objectMapper.writeValueAsString(Map.of("chosenOption", "A"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + part7QuestionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void saveAnswer_afterSubmission_returns409() throws Exception {
        String token = registerAndGetToken("s6a-submitted-answer@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(post("/attempts/" + attemptId + "/submit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String body = objectMapper.writeValueAsString(Map.of("chosenOption", "A"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict());
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

    @Test
    void submitAttempt_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/attempts/1/submit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitAttempt_onNonexistentAttempt_returns404() throws Exception {
        String token = registerAndGetToken("s8a-submit-404@example.com");

        mockMvc.perform(post("/attempts/999999/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitAttempt_onAnotherUsersAttempt_returns403() throws Exception {
        String ownerToken = registerAndGetToken("s8a-owner@example.com");
        String intruderToken = registerAndGetToken("s8a-intruder@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitAttempt_fullyAnsweredPartPractice_scoresCorrectly() throws Exception {
        String token = registerAndGetToken("s8a-full@example.com");
        Long correctQuestionId = seedQuestion("Part 5");
        Long wrongQuestionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // seedQuestion always uses correctOption = "A".
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + correctQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + wrongQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "B"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.answers.length()").value(2));
    }

    @Test
    void submitAttempt_withUnansweredQuestion_countsItAsIncorrect() throws Exception {
        String token = registerAndGetToken("s8a-unanswered@example.com");
        Long answeredQuestionId = seedQuestion("Part 5");
        seedQuestion("Part 5"); // left unanswered — decision #2
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + answeredQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());

        // totalQuestions comes from the Part's full question set
        // (QuestionRepository.findByPart), not just the one Answer row, so
        // the unanswered question still counts toward the denominator —
        // and never toward correctCount, since it has no Answer row at all.
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.answers.length()").value(1));
    }

    @Test
    void submitAttempt_calledTwice_isIdempotentAndIgnoresAnswersSavedAfterFirstSubmit() throws Exception {
        String token = registerAndGetToken("s8a-idempotent@example.com");
        Long firstQuestionId = seedQuestion("Part 5");
        Long secondQuestionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + firstQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2));

        // Approved decision #4: an answer saved after the first submit must
        // never change the persisted score on a later submit call.
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + secondQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2));
    }

    @Test
    void submitAttempt_withWrongAnswer_setsExplanationUnavailableWithNoApiKeyConfigured() throws Exception {
        String token = registerAndGetToken("s8b-explain-unavailable@example.com");
        Long correctQuestionId = seedQuestion("Part 5");
        Long wrongQuestionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + correctQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + wrongQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "B"))))
                .andExpect(status().isOk());

        String submitResponse = mockMvc.perform(post("/attempts/" + attemptId + "/submit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var answers = objectMapper.readTree(submitResponse).get("answers");
        for (var answer : answers) {
            long questionId = answer.get("questionId").asLong();
            if (questionId == wrongQuestionId) {
                // Approved decision #4/#7: no ANTHROPIC_API_KEY configured
                // under src/test -> the call is never attempted, and the
                // wrong answer's explanation deterministically resolves to
                // UNAVAILABLE rather than failing the whole submit.
                org.junit.jupiter.api.Assertions.assertEquals("UNAVAILABLE",
                        answer.get("explanationStatus").asText());
                org.junit.jupiter.api.Assertions.assertTrue(answer.get("explanationText").isNull());
            } else {
                // Correct answers never get an explanation request at all.
                org.junit.jupiter.api.Assertions.assertTrue(answer.get("explanationStatus").isNull());
                org.junit.jupiter.api.Assertions.assertTrue(answer.get("explanationText").isNull());
            }
        }
    }

    @Test
    void retryExplanation_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/attempts/1/answers/1/retry-explanation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retryExplanation_onNonexistentAttempt_returns404() throws Exception {
        String token = registerAndGetToken("s8b-retry-404@example.com");

        mockMvc.perform(post("/attempts/999999/answers/1/retry-explanation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void retryExplanation_onAnotherUsersAttempt_returns403() throws Exception {
        String ownerToken = registerAndGetToken("s8b-retry-owner@example.com");
        String intruderToken = registerAndGetToken("s8b-retry-intruder@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(post("/attempts/" + attemptId + "/answers/" + questionId + "/retry-explanation")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void retryExplanation_beforeAttemptIsSubmitted_returns409() throws Exception {
        String token = registerAndGetToken("s8b-retry-not-submitted@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(post("/attempts/" + attemptId + "/answers/" + questionId + "/retry-explanation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void retryExplanation_onNonexistentAnswer_returns404() throws Exception {
        String token = registerAndGetToken("s8b-retry-no-answer@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Submit with nothing answered: no Answer row exists for questionId.
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/attempts/" + attemptId + "/answers/" + questionId + "/retry-explanation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void retryExplanation_onCorrectAnswer_returns409() throws Exception {
        String token = registerAndGetToken("s8b-retry-correct@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // The answer was correct, so explanationStatus is null, not
        // UNAVAILABLE — approved decision #5: retry is rejected, not a
        // silent no-op, when there was never anything to retry.
        mockMvc.perform(post("/attempts/" + attemptId + "/answers/" + questionId + "/retry-explanation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void retryExplanation_onUnavailableWrongAnswer_retriesAndReturns200() throws Exception {
        String token = registerAndGetToken("s8b-retry-success@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "B"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // No API key configured here either, so the retry also resolves
        // to UNAVAILABLE again — but the endpoint itself must still
        // succeed (200), not error, since the retry attempt itself
        // completed normally.
        mockMvc.perform(post("/attempts/" + attemptId + "/answers/" + questionId + "/retry-explanation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(questionId))
                .andExpect(jsonPath("$.explanationStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.explanationText").value(nullValue()));
    }

    @Test
    void listHistory_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/attempts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listHistory_withNoSubmittedAttempts_returnsEmptyList() throws Exception {
        String token = registerAndGetToken("s10a-history-empty@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        // One IN_PROGRESS attempt exists, but history must not list it
        // (approved decision #1: SUBMITTED-only).
        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/attempts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listHistory_listsOnlySubmittedAttemptsMostRecentFirst() throws Exception {
        String token = registerAndGetToken("s10a-history@example.com");
        Long part5QuestionId = seedQuestion("Part 5");

        String startPart5Body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String part5Response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startPart5Body))
                .andReturn().getResponse().getContentAsString();
        long part5AttemptId = objectMapper.readTree(part5Response).get("id").asLong();
        mockMvc.perform(put("/attempts/" + part5AttemptId + "/answers/" + part5QuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/attempts/" + part5AttemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Long part6QuestionId = seedQuestion("Part 6");
        String startPart6Body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 6"));
        String part6Response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startPart6Body))
                .andReturn().getResponse().getContentAsString();
        long part6AttemptId = objectMapper.readTree(part6Response).get("id").asLong();
        mockMvc.perform(post("/attempts/" + part6AttemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // A second, still-IN_PROGRESS attempt that must not appear.
        seedQuestion("Part 7");
        String startPart7Body = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 7"));
        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startPart7Body))
                .andExpect(status().isCreated());

        // Most-recent-first: part6AttemptId was submitted after part5AttemptId.
        mockMvc.perform(get("/attempts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(part6AttemptId))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[1].id").value(part5AttemptId))
                .andExpect(jsonPath("$[1].score").value(1))
                .andExpect(jsonPath("$[1].totalQuestions").value(1));
    }

    @Test
    void listHistory_onlyReturnsCallersOwnAttempts() throws Exception {
        String ownerToken = registerAndGetToken("s10a-history-owner@example.com");
        String otherToken = registerAndGetToken("s10a-history-other@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/attempts").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deleteAttempt_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/attempts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAttempt_onNonexistentAttempt_returns404() throws Exception {
        String token = registerAndGetToken("s10a-delete-404@example.com");

        mockMvc.perform(delete("/attempts/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAttempt_onAnotherUsersAttempt_returns403() throws Exception {
        String ownerToken = registerAndGetToken("s10a-delete-owner@example.com");
        String intruderToken = registerAndGetToken("s10a-delete-intruder@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(delete("/attempts/" + attemptId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAttempt_onOwnInProgressAttempt_removesItAndItDisappearsFromGet() throws Exception {
        String token = registerAndGetToken("s10a-delete-in-progress@example.com");
        seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Approved decision #5: deleting an IN_PROGRESS (not just
        // SUBMITTED) attempt is allowed.
        mockMvc.perform(delete("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAttempt_onOwnSubmittedAttemptWithAnswers_cascadesToAnswersAndRemovesFromHistory() throws Exception {
        String token = registerAndGetToken("s10a-delete-cascade@example.com");
        Long questionId = seedQuestion("Part 5");
        String startBody = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 5"));
        String startResponse = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Approved decision #6: the Answer row is gone too, not just the
        // Attempt — deleting it again (or fetching it) confirms the
        // Attempt itself is gone (404), which is as far as this API
        // surface can directly observe the Answer-cascade from outside.
        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/attempts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---- S7b: questions on GET /attempts/{id} ----

    private long startAttemptAndGetId(String token, Map<String, String> startBody) throws Exception {
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(objectMapper.writeValueAsString(startBody)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void getAttempt_returnsTheQuestionsOfItsPart_inIdOrder_withParsedOptions_andNoAnswerKey() throws Exception {
        String token = registerAndGetToken("s7b-questions@example.com");
        Long first = questionRepository.save(new Question("Part 5", "Pick the right form.",
                "[\"submit\",\"to submit\",\"submitting\"]", "to submit")).getId();
        Long second = questionRepository.save(new Question("Part 5", "Second prompt.",
                "[\"A\",\"B\",\"C\",\"D\"]", "A")).getId();
        seedQuestion("Part 6"); // another Part: must not appear
        long attemptId = startAttemptAndGetId(token, Map.of("type", "PART_PRACTICE", "part", "Part 5"));

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(first))
                .andExpect(jsonPath("$.questions[0].prompt").value("Pick the right form."))
                .andExpect(jsonPath("$.questions[0].options[1]").value("to submit"))
                .andExpect(jsonPath("$.questions[0].options.length()").value(3))
                .andExpect(jsonPath("$.questions[1].id").value(second))
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist())
                .andExpect(jsonPath("$.questions[1].correctOption").doesNotExist());
    }

    @Test
    void getAttempt_onMockTest_returnsTheWholeQuestionBank_sinceS11() throws Exception {
        // Originally "...returnsNoQuestionsUntilS11": true when S7b was
        // written, false now that S11 exists — MOCK_TEST's assigned
        // question set is the whole bank (approved S11 decision #1), same
        // "whatever grading uses, the detail view shows" precedent S7b
        // established for PART_PRACTICE. Renamed and rewritten rather than
        // silently left to fail or silently deleted.
        String token = registerAndGetToken("s11-mock-questions@example.com");
        Long first = seedQuestion("Part 5");
        Long second = seedQuestion("Part 6");
        long attemptId = startAttemptAndGetId(token, Map.of("type", "MOCK_TEST"));

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(first))
                .andExpect(jsonPath("$.questions[1].id").value(second))
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist());
    }

    // ---- S11: mock test (full question bank, timed) ----

    @Test
    void startAttempt_mockTestWithEmptyQuestionBank_returns422() throws Exception {
        String token = registerAndGetToken("s11-empty-bank@example.com");
        String body = objectMapper.writeValueAsString(Map.of("type", "MOCK_TEST"));

        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void startAttempt_mockTestWithQuestions_createsInProgressAttemptWithTimeLimit() throws Exception {
        String token = registerAndGetToken("s11-start@example.com");
        seedQuestion("Part 5");
        String body = objectMapper.writeValueAsString(Map.of("type", "MOCK_TEST"));

        mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("MOCK_TEST"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getAttempt_onMockTest_returnsNonNullServerComputedRemainingSeconds() throws Exception {
        String token = registerAndGetToken("s11-remaining@example.com");
        seedQuestion("Part 5");
        long attemptId = startAttemptAndGetId(token, Map.of("type", "MOCK_TEST"));

        // Approved decision #4: a fixed 7200-second (2-hour) limit, just
        // started, so remaining time must be a real number close to 7200
        // — never null (that would mean "untimed", which MOCK_TEST no
        // longer is) and never more than the limit itself.
        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingSeconds").value(org.hamcrest.Matchers.lessThanOrEqualTo(7200)))
                .andExpect(jsonPath("$.remainingSeconds").value(org.hamcrest.Matchers.greaterThan(7000)));
    }

    @Test
    void startAttempt_mockTestCalledTwice_resumesSameAttempt() throws Exception {
        String token = registerAndGetToken("s11-resume@example.com");
        seedQuestion("Part 5");
        String body = objectMapper.writeValueAsString(Map.of("type", "MOCK_TEST"));

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
    void submitAttempt_mockTestAcrossMultipleParts_scoresAgainstTheWholeBank() throws Exception {
        String token = registerAndGetToken("s11-submit@example.com");
        Long correctQuestionId = seedQuestion("Part 5"); // correctOption = "A"
        seedQuestion("Part 6"); // left unanswered — must still count toward totalQuestions
        long attemptId = startAttemptAndGetId(token, Map.of("type", "MOCK_TEST"));

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + correctQuestionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());

        // Approved decisions #1/#2: totalQuestions for MOCK_TEST is now
        // the whole bank (2), not just the one answer saved — the
        // unanswered Part 6 question counts as incorrect, same mechanism
        // PART_PRACTICE already used.
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.answers.length()").value(1));
    }
}

