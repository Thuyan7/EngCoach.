package com.engcoach.tests;

import java.util.List;

public record ToeicTestView(Long id, String title, String description, List<TestPartView> parts) {
    public static ToeicTestView from(ToeicTest test, List<TestPart> parts) {
        return new ToeicTestView(test.getId(), test.getTitle(), test.getDescription(),
                parts.stream().map(TestPartView::from).toList());
    }
}
