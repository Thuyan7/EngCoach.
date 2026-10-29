package com.engcoach.attempts.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body for {@code POST /attempts}. {@code type} is required
 * ({@code PART_PRACTICE} or {@code MOCK_TEST}); {@code part} is required
 * for {@code PART_PRACTICE} and ignored for {@code MOCK_TEST} (checked in
 * AttemptsService, not here, since the rule depends on the value of
 * another field — bean validation alone can't express that cleanly).
 */
public class StartAttemptRequest {

    @NotBlank
    private String type;

    private String part;

    private List<String> parts = new ArrayList<>();

    public StartAttemptRequest() {
        // required for JSON deserialization
    }

    public StartAttemptRequest(String type, String part) {
        this.type = type;
        this.part = part;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getParts() { return parts; }

    public void setParts(List<String> parts) { this.parts = parts == null ? new ArrayList<>() : new ArrayList<>(parts); }

    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }
}
