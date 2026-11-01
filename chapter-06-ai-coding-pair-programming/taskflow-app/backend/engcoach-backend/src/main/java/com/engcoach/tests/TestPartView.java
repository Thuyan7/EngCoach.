package com.engcoach.tests;

public record TestPartView(String part, int questionCount, int durationSeconds) {
    public static TestPartView from(TestPart part) {
        return new TestPartView(part.getPart(), part.getQuestionCount(), part.getDurationSeconds());
    }
}
