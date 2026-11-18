package com.engcoach.streak;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StreakApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
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

    private long startPractice(String token) throws Exception {
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"type\":\"PART_PRACTICE\",\"part\":\"Part 5\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void answer(String token, long attemptId, Long questionId) throws Exception {
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"chosenOption\":\"A\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void newLearner_hasNoStreak_andAnonymousIsRejected() throws Exception {
        String token = token("streak-new@example.com");

        mockMvc.perform(get("/streak").param("timeZone", "UTC").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.longestStreak").value(0))
                .andExpect(jsonPath("$.studiedToday").value(false))
                .andExpect(jsonPath("$.studyDays.length()").value(0));

        mockMvc.perform(get("/streak")).andExpect(status().isUnauthorized());
    }

    @Test
    void answeringTodayMarksTodayAndStartsTheStreak_andEarlierDaysShowInTheirOwnMonth() throws Exception {
        String token = token("streak-days@example.com");
        Long q1 = question("Streak question 1");
        long attemptId = startPractice(token);
        answer(token, attemptId, q1);

        ZoneId utc = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(utc);
        mockMvc.perform(get("/streak").param("timeZone", "UTC").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.studiedToday").value(true))
                .andExpect(jsonPath("$.today").value(today.toString()))
                .andExpect(jsonPath("$.month").value(YearMonth.from(today).toString()))
                .andExpect(jsonPath("$.studyDays.length()").value(1))
                .andExpect(jsonPath("$.studyDays[0]").value(today.toString()));

        // Move the answer to two days ago: today is no longer studied, and the streak has been broken.
        jdbcTemplate.update("update answers set answered_at = ?",
                java.sql.Timestamp.from(today.minusDays(2).atTime(12, 0).atZone(utc).toInstant()));
        YearMonth earlier = YearMonth.from(today.minusDays(2));
        mockMvc.perform(get("/streak").param("timeZone", "UTC").param("month", earlier.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.studiedToday").value(false))
                .andExpect(jsonPath("$.month").value(earlier.toString()))
                .andExpect(jsonPath("$.studyDays[0]").value(today.minusDays(2).toString()));

        // A month without study has no marked days; a bad month or time zone falls back instead of failing.
        mockMvc.perform(get("/streak").param("timeZone", "UTC").param("month", "2001-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.studyDays.length()").value(0));
        mockMvc.perform(get("/streak").param("timeZone", "Not/AZone").param("month", "garbage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(YearMonth.from(LocalDate.now(utc)).toString()));

        // Another learner does not see these days.
        mockMvc.perform(get("/streak").param("timeZone", "UTC")
                        .header("Authorization", "Bearer " + token("streak-other@example.com")))
                .andExpect(jsonPath("$.longestStreak").value(0));
    }
}
