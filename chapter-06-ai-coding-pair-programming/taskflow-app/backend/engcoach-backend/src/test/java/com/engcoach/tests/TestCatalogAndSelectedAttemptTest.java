package com.engcoach.tests;

import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class TestCatalogAndSelectedAttemptTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ToeicTestRepository testRepository;
    @Autowired private TestPartRepository testPartRepository;
    @Autowired private TestQuestionRepository testQuestionRepository;
    @Autowired private QuestionGroupRepository questionGroupRepository;

    @BeforeEach
    void cleanTestFixtures() {
        testQuestionRepository.deleteAll();
        testPartRepository.deleteAll();
        testRepository.deleteAll();
        questionGroupRepository.deleteAll();
        questionRepository.deleteAll();
    }

    private String token(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "correct-horse-battery"));
        String response = mockMvc.perform(post("/auth/register")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private Long question(String part, String prompt) {
        return questionRepository.save(new Question(part, prompt,
                "[\\"A\\",\\"B\\",\\"C\\",\\"D\\"]", "A")).getId();
    }

    private Long createTest(Long part5Question, Long part7Question) {
        ToeicTest test = testRepository.save(new ToeicTest("Test 1", "Practice test", true));
        testPartRepository.saveAll(List.of(
                new TestPart(test.getId(), "Part 5", 1, 600, 5),
                new TestPart(test.getId(), "Part 7", 1, 1200, 7)));
        testQuestionRepository.saveAll(List.of(
                new TestQuestion(test.getId(), part5Question, null, 1),
                new TestQuestion(test.getId(), part7Question, null, 2)));
        return test.getId();
    }

    @Test
    void test01_listPublishedTests_returnsPartCatalog() throws Exception {
        ToeicTest test = testRepository.save(new ToeicTest("Visible Test", "Published", true));
        testPartRepository.save(new TestPart(test.getId(), "Part 5", 30, 600, 5));
        testRepository.save(new ToeicTest("Draft Test", "Hidden", false));

        String token = token("test01-catalog@example.com");
        mockMvc.perform(get("/tests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Visible Test"))
                .andExpect(jsonPath("$[0].parts[0].part").value("Part 5"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void test02_startSelectedParts_onlyAssignsQuestionsFromSelectedPart() throws Exception {
        String token = token("test02-select@example.com");
        Long part5 = question("Part 5", "Question from Part 5");
        Long part7 = question("Part 7", "Question from Part 7");
        Long testId = createTest(part5, part7);

        String body = objectMapper.writeValueAsString(Map.of(
                "type", "TEST", "testId", testId, "parts", List.of("Part 7")));
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TEST"))
                .andExpect(jsonPath("$.testId").value(testId))
                .andExpect(jsonPath("$.selectedParts[0]").value("Part 7"))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].id").value(part7))
                .andExpect(jsonPath("$.questions[0].prompt").value("Question from Part 7"));
    }

    @Test
    void test03_saveAndGrade_areRestrictedToSelectedParts() throws Exception {
        String token = token("test03-grade@example.com");
        Long part5 = question("Part 5", "Question from Part 5");
        Long part7 = question("Part 7", "Question from Part 7");
        Long testId = createTest(part5, part7);
        String startBody = objectMapper.writeValueAsString(Map.of(
                "type", "TEST", "testId", testId, "parts", List.of("Part 7")));
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(startBody))
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + part5)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + part7)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/attempts/" + attemptId + "/submit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(1));
    }
}
