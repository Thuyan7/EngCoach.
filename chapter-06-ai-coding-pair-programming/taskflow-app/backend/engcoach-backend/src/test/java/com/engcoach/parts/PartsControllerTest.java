package com.engcoach.parts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice S5a. implementation-plan.md's definition of done: "GET /parts
 * returns all distinct Parts with a valid JWT." The optional sample question
 * seeder is disabled by default, so these tests insert Question rows directly
 * via QuestionRepository rather than depending on sample data being present.
 *
 * Spring caches the test ApplicationContext (and so the same in-memory H2
 * instance) across test classes in one Maven run, and JUnit 5's default
 * method order is unspecified — so "no questions seeded" can't just assume
 * the table happens to be empty; it has to make that true itself.
 * {@code @BeforeEach} clears the table before every test in this class,
 * regardless of run order or what other test classes left behind.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PartsControllerTest {

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

    @Test
    void listParts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/parts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listParts_withNoQuestionsSeeded_returns200AndEmptyArray() throws Exception {
        String token = registerAndGetToken("parts-empty@example.com");

        mockMvc.perform(get("/parts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listParts_withQuestionsSeeded_returnsDistinctPartsOnly() throws Exception {
        String token = registerAndGetToken("parts-seeded@example.com");

        questionRepository.save(new Question("Part 5", "Choose the best word.",
                "[\"A\",\"B\",\"C\",\"D\"]", "B"));
        questionRepository.save(new Question("Part 5", "Another Part 5 question.",
                "[\"A\",\"B\",\"C\",\"D\"]", "A"));
        questionRepository.save(new Question("Part 3", "Listen to the conversation.",
                "[\"A\",\"B\",\"C\",\"D\"]", "C"));

        // Two "Part 5" rows must collapse to one entry — this is what makes
        // the test actually exercise DISTINCT rather than just "returns
        // whatever's in the table."
        mockMvc.perform(get("/parts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Part 3"))
                .andExpect(jsonPath("$[1]").value("Part 5"));
    }
}
