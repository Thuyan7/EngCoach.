package com.engcoach.parts;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteQuestionBankImporterTest {

    @Test
    void parsesJsonArrayWrappedInJavascriptAssignment() throws IOException {
        JsonNode bank = RemoteQuestionBankImporter.parseBankArray(
                "window.BUILTIN_BANK = [{\"part\":\"5\",\"prompt\":\"Choose.\","
                        + "\"choices\":[\"A\",\"B\"],\"answer\":1}];");

        assertEquals(1, bank.size());
        assertEquals("Choose.", bank.get(0).get("prompt").asText());
        assertEquals(1, bank.get(0).get("answer").asInt());
    }

    @Test
    void rejectsJavascriptWithoutAJsonArray() {
        assertThrows(IOException.class,
                () -> RemoteQuestionBankImporter.parseBankArray("window.BUILTIN_BANK = undefined;"));
    }

    @Test
    void parsesAnEmptyArrayButImporterRejectsItBeforeSaving() throws IOException {
        JsonNode bank = RemoteQuestionBankImporter.parseBankArray("window.BUILTIN_BANK = [];");
        assertEquals(0, bank.size());
    }
}
