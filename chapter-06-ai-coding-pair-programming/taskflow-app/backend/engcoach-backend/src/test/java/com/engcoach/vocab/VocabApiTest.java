package com.engcoach.vocab;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VocabApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WordLookupSource lookupSource;

    private String token(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-battery"));
        String response = mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    private String json(String word, String meaning, String example) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("word", word);
        body.put("meaning", meaning);
        if (example != null) body.put("example", example);
        return objectMapper.writeValueAsString(body);
    }

    private long add(String token, String word, String meaning) throws Exception {
        String response = mockMvc.perform(post("/vocab").header("Authorization", auth(token))
                        .contentType("application/json").content(json(word, meaning, null)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }


    @Test
    void anonymousCallersAreRejected() throws Exception {
        mockMvc.perform(get("/vocab")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/vocab/review")).andExpect(status().isUnauthorized());
    }

    @Test
    void addingAWord_savesItDueNowInBoxOne_rejectsDuplicatesAndInvalidInput() throws Exception {
        String token = token("vocab-add@example.com");

        mockMvc.perform(post("/vocab").header("Authorization", auth(token)).contentType("application/json")
                        .content(json("  Invoice ", "hoa don", "Please send the invoice.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.word").value("Invoice"))
                .andExpect(jsonPath("$.meaning").value("hoa don"))
                .andExpect(jsonPath("$.example").value("Please send the invoice."))
                .andExpect(jsonPath("$.box").value(1))
                .andExpect(jsonPath("$.due").value(true))
                .andExpect(jsonPath("$.reviewCount").value(0));

        // The same word in another case is a duplicate; blank or oversized fields are invalid.
        mockMvc.perform(post("/vocab").header("Authorization", auth(token)).contentType("application/json")
                        .content(json("INVOICE", "another", null)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/vocab").header("Authorization", auth(token)).contentType("application/json")
                        .content(json("budget", "   ", null)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/vocab").header("Authorization", auth(token)).contentType("application/json")
                        .content(json("x".repeat(101), "too long", null)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/vocab").header("Authorization", auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.dueCount").value(1))
                .andExpect(jsonPath("$.words[0].word").value("Invoice"));
    }

    @Test
    void reviewingACard_movesItBetweenBoxesAndRemovesItFromTheDeck() throws Exception {
        String token = token("vocab-review@example.com");
        long id = add(token, "deadline", "han chot");

        mockMvc.perform(get("/vocab/review").header("Authorization", auth(token)))
                .andExpect(jsonPath("$.dueCount").value(1))
                .andExpect(jsonPath("$.cards.length()").value(1));

        // Remembered: box 2, next review in days, so the deck is empty.
        mockMvc.perform(post("/vocab/" + id + "/review").header("Authorization", auth(token))
                        .contentType("application/json").content("{\"remembered\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.box").value(2))
                .andExpect(jsonPath("$.due").value(false))
                .andExpect(jsonPath("$.reviewCount").value(1));
        mockMvc.perform(get("/vocab/review").header("Authorization", auth(token)))
                .andExpect(jsonPath("$.dueCount").value(0))
                .andExpect(jsonPath("$.cards.length()").value(0));

        // Forgotten: back to box 1, due again in a few minutes (not now).
        mockMvc.perform(post("/vocab/" + id + "/review").header("Authorization", auth(token))
                        .contentType("application/json").content("{\"remembered\":false}"))
                .andExpect(jsonPath("$.box").value(1))
                .andExpect(jsonPath("$.due").value(false))
                .andExpect(jsonPath("$.reviewCount").value(2));

        mockMvc.perform(post("/vocab/" + id + "/review").header("Authorization", auth(token))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theDeckIsLimited_butItReportsEveryDueWord() throws Exception {
        String token = token("vocab-deck@example.com");
        add(token, "alpha", "a");
        add(token, "beta", "b");
        add(token, "gamma", "c");

        mockMvc.perform(get("/vocab/review").param("limit", "2").header("Authorization", auth(token)))
                .andExpect(jsonPath("$.dueCount").value(3))
                .andExpect(jsonPath("$.cards.length()").value(2));
    }

    @Test
    void words_canBeEditedAndDeleted_withDuplicateAndMissingChecks() throws Exception {
        String token = token("vocab-edit@example.com");
        long first = add(token, "refund", "hoan tien");
        add(token, "receipt", "bien lai");

        mockMvc.perform(put("/vocab/" + first).header("Authorization", auth(token)).contentType("application/json")
                        .content(json("refund", "tra lai tien", "A full refund.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meaning").value("tra lai tien"))
                .andExpect(jsonPath("$.example").value("A full refund."));
        mockMvc.perform(put("/vocab/" + first).header("Authorization", auth(token)).contentType("application/json")
                        .content(json("Receipt", "clash", null)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/vocab/" + first).header("Authorization", auth(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/vocab/" + first).header("Authorization", auth(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vocab").header("Authorization", auth(token)))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void anotherLearnerCannotSeeOrChangeMyWords() throws Exception {
        String mine = token("vocab-owner@example.com");
        String other = token("vocab-other@example.com");
        long id = add(mine, "contract", "hop dong");

        mockMvc.perform(get("/vocab").header("Authorization", auth(other)))
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(put("/vocab/" + id).header("Authorization", auth(other)).contentType("application/json")
                        .content(json("contract", "stolen", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/vocab/" + id).header("Authorization", auth(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vocab/" + id + "/review").header("Authorization", auth(other))
                        .contentType("application/json").content("{\"remembered\":true}"))
                .andExpect(status().isNotFound());

        // The same word can be saved independently by the other learner.
        add(other, "contract", "own meaning");
        mockMvc.perform(get("/vocab").header("Authorization", auth(mine)))
                .andExpect(jsonPath("$.words[0].meaning").value("hop dong"));
    }

    @Test
    void lookupSuggestsAMeaningForASelectedWord() throws Exception {
        String token = token("lookup@example.com");
        org.mockito.Mockito.when(lookupSource.find("invoice", "Please bring the invoice."))
                .thenReturn(new WordLookup("hoa don", "noun", "/x/"));

        mockMvc.perform(get("/vocab/lookup").param("word", " invoice ").param("context", "Please bring the invoice.")
                        .header("Authorization", auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meaning").value("hoa don"))
                .andExpect(jsonPath("$.partOfSpeech").value("noun"))
                .andExpect(jsonPath("$.phonetic").value("/x/"));
    }

    @Test
    void lookupIsEmptyForBlankOrTooLongWordsAndNeedsSignIn() throws Exception {
        String token = token("lookup2@example.com");
        mockMvc.perform(get("/vocab/lookup").param("word", "   ").header("Authorization", auth(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meaning").doesNotExist());
        mockMvc.perform(get("/vocab/lookup").param("word", "a".repeat(101)).header("Authorization", auth(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meaning").doesNotExist());
        mockMvc.perform(get("/vocab/lookup").param("word", "invoice")).andExpect(status().isUnauthorized());
    }
}
