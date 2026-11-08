package com.engcoach.tests;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_parts")
public class TestPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(nullable = false, length = 16)
    private String part;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TestPart() { }

    public TestPart(Long testId, String part, int questionCount, int durationSeconds, int sortOrder) {
        this.testId = testId;
        this.part = part;
        this.questionCount = questionCount;
        this.durationSeconds = durationSeconds;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getTestId() { return testId; }
    public String getPart() { return part; }
    public int getQuestionCount() { return questionCount; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getSortOrder() { return sortOrder; }
}
