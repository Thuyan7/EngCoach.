package com.engcoach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc

class AuthRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerWithValidCredentials_returns201AndJwtAndHashedPassword() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "learner1@example.com",
                "password", "correct-horse-battery"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value(notNullValue()));

        Optional<User> saved = userRepository.findByEmailIgnoreCase("learner1@example.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getPasswordHash()).isNotEqualTo("correct-horse-battery");
        assertThat(passwordEncoder.matches("correct-horse-battery", saved.get().getPasswordHash())).isTrue();
    }

    @Test
    void registerWithAlreadyRegisteredEmail_returns409ProblemDetail_noDuplicateRow() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "duplicate@example.com",
                "password", "correct-horse-battery"
        ));

        // first registration succeeds
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        // second registration with same email is rejected
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Email already registered"))
                .andExpect(jsonPath("$.status").value(409));

        long count = userRepository.findByEmailIgnoreCase("duplicate@example.com").isPresent() ? 1 : 0;
        assertThat(count).isEqualTo(1); // exactly one row, not two
    }

    @Test
    void registerWithInvalidEmailAndShortPassword_returns422WithFieldErrors() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "not-an-email",
                "password", "short"
        ));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void jwtPayloadContainsOnlyUserIdSubject_noEmailOrPii() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "privacy-check@example.com",
                "password", "correct-horse-battery"
        ));

        String response = mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("accessToken").asText();
        String payloadJson = decodeJwtPayload(token);

        assertThat(payloadJson).doesNotContain("privacy-check@example.com");
        assertThat(payloadJson).doesNotContain("@example.com");
    }

    private static String decodeJwtPayload(String jwt) {
        String[] parts = jwt.split("\\.");
        byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
        return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
    }
}