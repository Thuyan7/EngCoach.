package com.engcoach.attempts;

import com.engcoach.parts.Question;
import com.engcoach.parts.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration regression test for the question payload returned to an active
 * test taker. The correct answer is stored on the entity but must not be
 * exposed by GET /attempts/{id} before the attempt is submitted.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttemptQuestionPayloadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearQuestions() {
        questionRepository.deleteAll();
    }

    private String registerAndGetToken() throws Exception {
        String email = "question-payload-" + UUID.randomUUID() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "correct-horse-battery"
        ));
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void attemptDetailIncludesReadingContextButNeverTheCorrectAnswer() throws Exception {
        questionRepository.save(new Question(
                "Part 7",
                "What is the notice about?",
                "[\"An office renovation\",\"A holiday schedule\"]",
                "An office renovation",
                "NOTICE TO STAFF: The second-floor offices will be renovated.",
                "https://example.com/office.jpg",
                "https://example.com/notice.mp3"));

        String token = registerAndGetToken();
        String startBody = objectMapper.writeValueAsString(
                Map.of("type", "PART_PRACTICE", "part", "Part 7"));
        String startResponse = mockMvc.perform(post("/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(get("/attempts/" + attemptId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].prompt").value("What is the notice about?"))
                .andExpect(jsonPath("$.questions[0].options.length()").value(2))
                .andExpect(jsonPath("$.questions[0].passage")
                        .value("NOTICE TO STAFF: The second-floor offices will be renovated."))
                .andExpect(jsonPath("$.questions[0].imageUrl").value("https://example.com/office.jpg"))
                .andExpect(jsonPath("$.questions[0].audioUrl").value("https://example.com/notice.mp3"))
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist());
    }
}
