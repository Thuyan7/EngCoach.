package com.engcoach.goals;

import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import com.engcoach.tests.QuestionGroupRepository;
import com.engcoach.tests.TestPartRepository;
import com.engcoach.tests.TestQuestionRepository;
import com.engcoach.tests.ToeicTestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GoalsApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ToeicTestRepository testRepository;
    @Autowired private TestPartRepository testPartRepository;
    @Autowired private TestQuestionRepository testQuestionRepository;
    @Autowired private QuestionGroupRepository questionGroupRepository;

    @BeforeEach
    void cleanQuestionBank() {
        testQuestionRepository.deleteAll();
        testPartRepository.deleteAll();
        testRepository.deleteAll();
        questionGroupRepository.deleteAll();
        questionRepository.deleteAll();
    }

    private String token(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-battery"));
        String response = mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private Long question(String prompt) throws Exception {
        String options = objectMapper.writeValueAsString(List.of("A", "B", "C", "D"));
        return questionRepository.save(new Question("Part 5", prompt, options, "A")).getId();
    }

    private void putGoals(String token, Object body, int expectedStatus) throws Exception {
        mockMvc.perform(put("/goals").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus));
    }

    private void answer(String token, long attemptId, Long questionId, String option) throws Exception {
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", option))))
                .andExpect(status().isOk());
    }

    @Test
    void newLearner_getsDefaultsAndNoProgress_andAnonymousIsRejected() throws Exception {
        String token = token("goals-default@example.com");

        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetScore").doesNotExist())
                .andExpect(jsonPath("$.dailyQuestionGoal").value(20))
                .andExpect(jsonPath("$.answeredToday").value(0))
                .andExpect(jsonPath("$.dailyGoalMet").value(false))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.longestStreak").value(0))
                .andExpect(jsonPath("$.estimatedScore").doesNotExist());

        mockMvc.perform(get("/goals")).andExpect(status().isUnauthorized());
    }

    @Test
    void goals_canBeSavedChangedAndCleared_andInvalidValuesAreRejected() throws Exception {
        String token = token("goals-save@example.com");

        putGoals(token, Map.of("targetScore", 700, "dailyQuestionGoal", 30), 200);
        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.targetScore").value(700))
                .andExpect(jsonPath("$.dailyQuestionGoal").value(30));

        // Changing again updates the same goals instead of adding a second row.
        putGoals(token, Map.of("targetScore", 850, "dailyQuestionGoal", 10), 200);
        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.targetScore").value(850))
                .andExpect(jsonPath("$.dailyQuestionGoal").value(10));

        // Leaving the target out clears it.
        putGoals(token, Map.of("dailyQuestionGoal", 10), 200);
        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.targetScore").doesNotExist());

        putGoals(token, Map.of("targetScore", 5, "dailyQuestionGoal", 10), 422);
        putGoals(token, Map.of("targetScore", 1000, "dailyQuestionGoal", 10), 422);
        putGoals(token, Map.of("dailyQuestionGoal", 0), 422);
        putGoals(token, Map.of("dailyQuestionGoal", 501), 422);
        putGoals(token, Map.of("targetScore", 700), 422);
    }

    @Test
    void answeredQuestions_countTowardTodayAndStartTheStreak_butReAnsweringDoesNotCountTwice() throws Exception {
        String token = token("goals-progress@example.com");
        Long q1 = question("Goal question 1");
        Long q2 = question("Goal question 2");
        putGoals(token, Map.of("dailyQuestionGoal", 2), 200);

        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"type\":\"PART_PRACTICE\",\"part\":\"Part 5\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(response).get("id").asLong();

        answer(token, attemptId, q1, "A");
        answer(token, attemptId, q1, "B");
        mockMvc.perform(get("/goals").param("timeZone", "UTC").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.answeredToday").value(1))
                .andExpect(jsonPath("$.dailyGoalMet").value(false))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1));

        answer(token, attemptId, q2, "C");
        mockMvc.perform(get("/goals").param("timeZone", "UTC").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.answeredToday").value(2))
                .andExpect(jsonPath("$.dailyGoalMet").value(true));

        // Another learner has none of this progress, and an unknown time zone falls back to UTC.
        String other = token("goals-other@example.com");
        mockMvc.perform(get("/goals").param("timeZone", "Not/AZone").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredToday").value(0))
                .andExpect(jsonPath("$.currentStreak").value(0));
    }
}
