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
        try {
            String options = objectMapper.writeValueAsString(List.of("A", "B", "C", "D"));
            return questionRepository.save(new Question(part, prompt, options, "A")).getId();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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

    @Test
    void partPractice_usesTheQuestionsOfTestsWithTheirGroup_notOldBankQuestions() throws Exception {
        String token = token("part-practice-groups@example.com");
        Long oldQuestion = question("Part 3", "Old bank question without media");
        Long linkedA = question("Part 3", "Conversation question A");
        Long linkedB = question("Part 3", "Conversation question B");
        ToeicTest test = testRepository.save(new ToeicTest("Generated Test", "d", true));
        testPartRepository.save(new TestPart(test.getId(), "Part 3", 2, 600, 3));
        QuestionGroup group = questionGroupRepository.save(new QuestionGroup("Part 3", "CONVERSATION", "Shared script.",
                null, "/audio/test-01/g1.mp3"));
        testQuestionRepository.saveAll(List.of(
                new TestQuestion(test.getId(), linkedA, group.getId(), 1),
                new TestQuestion(test.getId(), linkedB, group.getId(), 2)));

        String start = objectMapper.writeValueAsString(Map.of("type", "PART_PRACTICE", "part", "Part 3"));
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(start))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/attempts/" + attemptId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(linkedA))
                .andExpect(jsonPath("$.questions[0].groupType").value("CONVERSATION"))
                .andExpect(jsonPath("$.questions[0].audioUrl").value("/audio/test-01/g1.mp3"))
                .andExpect(jsonPath("$.questions[1].id").value(linkedB));

        // An old-bank question outside every Test is not part of this practice set, so it cannot be answered here.
        String bad = objectMapper.writeValueAsString(Map.of("chosenOption", "A"));
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + oldQuestion)
                        .header("Authorization", "Bearer " + token).contentType("application/json").content(bad))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + linkedA)
                        .header("Authorization", "Bearer " + token).contentType("application/json").content(bad))
                .andExpect(status().isOk());
    }

    @Test
    void scoreAndPartStats_countUnansweredAsWrongAndEstimateTheToeicScore() throws Exception {
        String token = token("score-and-stats@example.com");
        ToeicTest test = testRepository.save(new ToeicTest("Scored Test", "d", true));
        List<String> parts = List.of("Part 1", "Part 2", "Part 3", "Part 4", "Part 5", "Part 6", "Part 7");
        Map<String, List<Long>> byPart = new java.util.HashMap<>();
        int order = 1;
        for (String part : parts) {
            testPartRepository.save(new TestPart(test.getId(), part, 2, 600, Integer.parseInt(part.substring(5))));
            for (int n = 1; n <= 2; n++) {
                Long id = question(part, part + " question " + n);
                byPart.computeIfAbsent(part, key -> new java.util.ArrayList<>()).add(id);
                testQuestionRepository.save(new TestQuestion(test.getId(), id, null, order++));
            }
        }

        String start = objectMapper.writeValueAsString(Map.of("type", "TEST", "testId", test.getId(), "parts", parts));
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(start))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(response).get("id").asLong();

        // Part 1: both right, Part 2: one right, everything else left unanswered.
        List<Long> answered = List.of(byPart.get("Part 1").get(0), byPart.get("Part 1").get(1), byPart.get("Part 2").get(0));
        for (Long questionId : answered) {
            mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + questionId)
                            .header("Authorization", "Bearer " + token).contentType("application/json")
                            .content(objectMapper.writeValueAsString(Map.of("chosenOption", "A"))))
                    .andExpect(status().isOk());
        }

        // Before submitting there is nothing to score yet.
        mockMvc.perform(get("/attempts/" + attemptId + "/score").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parts.length()").value(0))
                .andExpect(jsonPath("$.estimate.total").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/attempts/" + attemptId + "/score").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parts.length()").value(7))
                .andExpect(jsonPath("$.parts[0].part").value("Part 1"))
                .andExpect(jsonPath("$.parts[0].correct").value(2))
                .andExpect(jsonPath("$.parts[0].accuracyPercent").value(100))
                .andExpect(jsonPath("$.parts[1].correct").value(1))
                .andExpect(jsonPath("$.parts[1].total").value(2))
                .andExpect(jsonPath("$.parts[1].accuracyPercent").value(50))
                .andExpect(jsonPath("$.parts[4].correct").value(0))
                // Listening: 3 right of 8 -> 190; Reading: 0 of 6 -> 5; total 195.
                .andExpect(jsonPath("$.estimate.listening").value(190))
                .andExpect(jsonPath("$.estimate.reading").value(5))
                .andExpect(jsonPath("$.estimate.total").value(195));

        mockMvc.perform(get("/attempts/part-stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptsCounted").value(1))
                .andExpect(jsonPath("$.parts.length()").value(7))
                .andExpect(jsonPath("$.latestEstimate.total").value(195))
                // Every Part has only 2 questions, too few to name a weakest Part.
                .andExpect(jsonPath("$.weakestPart").value(org.hamcrest.Matchers.nullValue()));

        // Another user cannot read this attempt's score.
        String other = token("score-other@example.com");
        mockMvc.perform(get("/attempts/" + attemptId + "/score").header("Authorization", "Bearer " + other))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/attempts/part-stats").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptsCounted").value(0))
                .andExpect(jsonPath("$.parts.length()").value(0));
        mockMvc.perform(get("/attempts/" + attemptId + "/score"))
                .andExpect(status().isUnauthorized());
    }

    private long startAndAnswer(String token, Long testId, List<String> parts, Map<Long, String> choices) throws Exception {
        String start = objectMapper.writeValueAsString(Map.of("type", "TEST", "testId", testId, "parts", parts));
        String response = mockMvc.perform(post("/attempts").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(start))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(response).get("id").asLong();
        for (Map.Entry<Long, String> choice : choices.entrySet()) {
            mockMvc.perform(put("/attempts/" + attemptId + "/answers/" + choice.getKey())
                            .header("Authorization", "Bearer " + token).contentType("application/json")
                            .content(objectMapper.writeValueAsString(Map.of("chosenOption", choice.getValue()))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/attempts/" + attemptId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return attemptId;
    }

    @Test
    void mistakes_listLatestWrongAnswers_filterByPart_andDropQuestionsLaterAnsweredRight() throws Exception {
        String token = token("mistakes@example.com");
        ToeicTest test = testRepository.save(new ToeicTest("Mistakes Test", "d", true));
        testPartRepository.save(new TestPart(test.getId(), "Part 5", 2, 600, 5));
        testPartRepository.save(new TestPart(test.getId(), "Part 7", 1, 600, 7));
        Long q1 = question("Part 5", "Part 5 question 1");
        Long q2 = question("Part 5", "Part 5 question 2");
        Long q3 = question("Part 7", "Part 7 question 1");
        testQuestionRepository.save(new TestQuestion(test.getId(), q1, null, 1));
        testQuestionRepository.save(new TestQuestion(test.getId(), q2, null, 2));
        testQuestionRepository.save(new TestQuestion(test.getId(), q3, null, 3));
        List<String> parts = List.of("Part 5", "Part 7");

        // Nothing submitted yet: no mistakes.
        mockMvc.perform(get("/attempts/mistakes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.mistakes.length()").value(0));

        // First attempt: q1 wrong, q2 right, q3 wrong. The correct answer is "A".
        startAndAnswer(token, test.getId(), parts, Map.of(q1, "B", q2, "A", q3, "C"));

        mockMvc.perform(get("/attempts/mistakes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.partCounts.length()").value(2))
                .andExpect(jsonPath("$.mistakes.length()").value(2));

        mockMvc.perform(get("/attempts/mistakes").param("part", "Part 5").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.partCounts.length()").value(2))
                .andExpect(jsonPath("$.mistakes.length()").value(1))
                .andExpect(jsonPath("$.mistakes[0].part").value("Part 5"))
                .andExpect(jsonPath("$.mistakes[0].chosenOption").value("B"))
                .andExpect(jsonPath("$.mistakes[0].question.correctOption").value("A"));

        // Second attempt: q1 answered correctly, so it leaves the list; q3 is still wrong.
        startAndAnswer(token, test.getId(), parts, Map.of(q1, "A", q3, "D"));

        mockMvc.perform(get("/attempts/mistakes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.mistakes[0].part").value("Part 7"))
                .andExpect(jsonPath("$.mistakes[0].chosenOption").value("D"));

        // Other users see nothing; anonymous callers are rejected.
        mockMvc.perform(get("/attempts/mistakes").header("Authorization", "Bearer " + token("mistakes-other@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/attempts/mistakes")).andExpect(status().isUnauthorized());
    }
}
