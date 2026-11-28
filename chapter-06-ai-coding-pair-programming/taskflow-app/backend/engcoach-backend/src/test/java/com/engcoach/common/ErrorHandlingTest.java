package com.engcoach.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7.3 / finding R4: every failure must be an RFC 9457 problem. Covers the
 * three gaps (malformed JSON, database constraint violation, unexpected
 * exception) and checks that the status codes of framework errors that were
 * already handled by Spring do not change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ErrorHandlingTest.FailingController.class)
class ErrorHandlingTest {

    /** Test-only endpoints that throw, so the handlers can be exercised end to end. */
    @RestController
    static class FailingController {
        @GetMapping("/test-only/integrity")
        String integrity() {
            throw new DataIntegrityViolationException("could not execute statement uk_secret_constraint");
        }

        @GetMapping("/test-only/boom")
        String boom() {
            throw new IllegalStateException("internal detail: db password is 1234");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-battery"));
        String response = mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void malformedJsonBody_returns400Problem() throws Exception {
        mockMvc.perform(post("/auth/register").contentType("application/json").content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://engcoach.app/problems/malformed-request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/auth/register"));
    }

    @Test
    void databaseConstraintViolation_returns409ProblemWithoutInternals() throws Exception {
        String token = registerAndGetToken("r4-integrity@example.com");

        mockMvc.perform(get("/test-only/integrity").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://engcoach.app/problems/data-conflict"))
                .andExpect(jsonPath("$.detail").value(not(containsString("uk_secret_constraint"))));
    }

    @Test
    void unexpectedException_returns500ProblemWithoutInternals() throws Exception {
        String token = registerAndGetToken("r4-boom@example.com");

        mockMvc.perform(get("/test-only/boom").header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://engcoach.app/problems/internal-error"))
                .andExpect(jsonPath("$.detail").value(not(containsString("1234"))));
    }

    // Invariant: statuses of errors Spring already handled must stay the same.
    @Test
    void unsupportedHttpMethod_stillReturns405() throws Exception {
        mockMvc.perform(delete("/auth/register"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void nonNumericPathId_stillReturns400() throws Exception {
        String token = registerAndGetToken("r4-typemismatch@example.com");

        mockMvc.perform(get("/attempts/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRegistration_stillReturns422WithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register").contentType("application/json")
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://engcoach.app/problems/validation-error"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }
}
