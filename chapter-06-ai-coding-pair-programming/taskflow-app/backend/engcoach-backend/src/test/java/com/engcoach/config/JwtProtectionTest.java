package com.engcoach.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice S3: proves JwtAuthenticationFilter + RestAuthenticationEntryPoint
 * actually gate every non-/auth route, using /attempts as the stand-in
 * protected path — that resource doesn't exist until S6a, so a request
 * that gets past authentication correctly 404s (no controller mapped yet)
 * instead of ever reaching real business logic. That 404-instead-of-401 is
 * exactly what proves the filter let an authenticated request through; it
 * is not itself part of this slice's contract and will naturally become a
 * real 2xx/4xx once /attempts is implemented.
 *
 * The one thing this slice's definition of done does NOT cover yet: "a
 * request for another user's attempt returns 403, not 404." That requires
 * a real owned resource (the Attempt entity, S6a+) and is intentionally
 * left unverified here — see SLICE-S3-NOTES.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "correct-horse-battery"
        ));
        String response = mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void protectedRouteWithNoAuthorizationHeader_returns401GenericProblemDetail() throws Exception {
        mockMvc.perform(get("/attempts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedRouteWithMalformedBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/attempts").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void protectedRouteWithNonBearerAuthorizationHeader_returns401() throws Exception {
        // e.g. Basic auth, or any scheme other than Bearer — must not be
        // treated as a token to try to parse.
        mockMvc.perform(get("/attempts").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithValidBearerToken_passesAuthenticationAnd404sOnTheMissingController() throws Exception {
        String token = registerAndGetToken("jwt-filter-check@example.com");

        // 404, not 401/403: the token was valid and authentication
        // succeeded, so the request reached the DispatcherServlet — it
        // just has nowhere to go yet, because /attempts isn't built until
        // S6a. This is the evidence that a valid token actually works.
        mockMvc.perform(get("/attempts").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerAndLoginRemainPublic_noTokenRequired() throws Exception {
        // Regression check: S3 must not accidentally start requiring a
        // token on the two routes S1/S2 explicitly made public.
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "still-public@example.com",
                "password", "correct-horse-battery"
        ));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk());
    }
}
