package com.engcoach.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * P7.3 / finding R1, invariant: with no engcoach.cors.allowed-origins
 * configured (local development behind the Vite proxy), no origin is allowed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsDisabledByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withoutConfiguredOrigins_noOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/attempts")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
