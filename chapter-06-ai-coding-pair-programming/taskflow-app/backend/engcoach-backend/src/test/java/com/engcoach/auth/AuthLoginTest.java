package com.engcoach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void registerUser(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
        ));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void loginWithCorrectCredentials_returns200AndJwt() throws Exception {
        registerUser("learner2@example.com", "correct-horse-battery");

        String body = objectMapper.writeValueAsString(Map.of(
                "email", "learner2@example.com",
                "password", "correct-horse-battery"
        ));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value(notNullValue()));
    }

    @Test
    void loginWithWrongPassword_returns401GenericProblemDetail() throws Exception {
        registerUser("learner3@example.com", "correct-horse-battery");

        String body = objectMapper.writeValueAsString(Map.of(
                "email", "learner3@example.com",
                "password", "totally-wrong-password"
        ));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid credentials"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void loginWithUnknownEmail_returns401WithSameGenericMessageAsWrongPassword() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "never-registered@example.com",
                "password", "whatever-password"
        ));

        // Same status, same title, same detail as a wrong-password response —
        // feature-specification.md §2: no field disclosure, so an attacker
        // can't use this response to tell "unknown email" apart from
        // "wrong password".
        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void loginWithMissingFields_returns401NotServerError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "learner4@example.com"
                // password omitted entirely
        ));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }
}
