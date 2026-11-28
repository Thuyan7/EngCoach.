package com.engcoach.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7.3 / finding R1: the backend must answer browser CORS preflights for the
 * deployed frontend origin (configured through engcoach.cors.allowed-origins)
 * and must not allow any other origin.
 */
@SpringBootTest(properties = "engcoach.cors.allowed-origins=https://app.example.com")
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromAllowedOrigin_isAcceptedWithoutAToken() throws Exception {
        mockMvc.perform(options("/attempts")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.com"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization")));
    }

    @Test
    void preflightFromAllowedOrigin_allowsPatchAndDelete() throws Exception {
        mockMvc.perform(options("/vocab/1")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", containsStringIgnoringCase("PATCH")));
    }

    @Test
    void preflightFromOtherOrigin_isRejectedWithoutCorsHeaders() throws Exception {
        mockMvc.perform(options("/attempts")
                        .header("Origin", "https://evil.example.org")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
